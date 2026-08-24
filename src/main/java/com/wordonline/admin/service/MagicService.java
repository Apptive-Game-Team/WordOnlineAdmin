package com.wordonline.admin.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.CardDto;
import com.wordonline.admin.dto.MagicDto;
import com.wordonline.admin.dto.MagicComparisonDto;
import com.wordonline.admin.dto.MagicCardComparisonDto;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.magic.MagicAccessType;
import com.wordonline.admin.entity.magic.MagicCard;
import com.wordonline.admin.entity.magic.MagicCastType;
import com.wordonline.admin.repository.magic.CardRepository;
import com.wordonline.admin.repository.magic.MagicCardRepository;
import com.wordonline.admin.repository.magic.MagicRepository;

import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MagicService {

    private final MagicRepository magicRepository;
    private final CardRepository cardRepository;
    private final MagicCardRepository magicCardRepository;
    private final Optional<SecondaryAdminDataService> secondaryAdminDataService;

    public boolean hasSecondaryDatabase() {
        return secondaryAdminDataService.isPresent();
    }

    @Transactional(readOnly = true)
    public List<MagicDto> getAllMagic() {
        return getAllMagic(false);
    }

    @Transactional(readOnly = true)
    public List<MagicDto> getAllMagic(boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().getMagics();
        }

        return magicRepository.findAllByOrderByIdAsc()
                .stream()
                .map(MagicDto::new)
                .toList();
    }

    public void addCardToMagic(long magicId, long cardId) {
        addCardToMagic(magicId, cardId, false);
    }

    public void addCardToMagic(long magicId, long cardId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().addCardToMagic(magicId, cardId);
            return;
        }

        Magic magic = magicRepository.findById(magicId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        MagicCard magicCard = new MagicCard(null, magic, card);

        magicCardRepository.save(magicCard);
        magic.addMagicCard(magicCard);
    }

    public void removeCardFromMagic(long magicId, long cardId) {
        removeCardFromMagic(magicId, cardId, false);
    }

    public void removeCardFromMagic(long magicId, long cardId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().removeCardFromMagic(magicId, cardId);
            return;
        }

        Magic magic = magicRepository.findById(magicId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        MagicCard magicCard = magic.removeOneMagicCard(cardId);

        if (magicCard == null) {
            return;
        }

        magicCardRepository.delete(magicCard);
    }

    public void removeMagic(long magicId) {
        removeMagic(magicId, false);
    }

    public void removeMagic(long magicId, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteMagic(magicId);
            return;
        }
        magicRepository.deleteById(magicId);
    }

    public void updateMagic(long magicId, String name, String castType, String accessType, boolean secondary) {
        String storedCastType = MagicCastType.requireStoredValue(castType);
        String storedAccessType = MagicAccessType.storableValueOrDefault(accessType);

        if (secondary) {
            rejectRowTheDatabaseRefuses(
                    name,
                    () -> secondaryAdminDataService.orElseThrow()
                            .updateMagic(magicId, name, storedCastType, storedAccessType)
            );
            return;
        }

        Magic magic = magicRepository.findById(magicId)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + magicId));
        applyAndFlush(magic, name, storedCastType, storedAccessType);
    }

    public void createMagic(String name, String castType, String accessType, boolean secondary) {
        String storedCastType = MagicCastType.requireStoredValue(castType);
        String storedAccessType = MagicAccessType.storableValueOrDefault(accessType);

        if (secondary) {
            rejectRowTheDatabaseRefuses(
                    name,
                    () -> secondaryAdminDataService.orElseThrow()
                            .createMagic(name, storedCastType, storedAccessType)
            );
            return;
        }

        Magic magic = new Magic();
        magic.setName(name);
        magic.setCastType(storedCastType);
        magic.setAccessType(storedAccessType);
        // saveAndFlush so a CHECK violation fails here, where it can still become a readable
        // message, instead of at commit time outside this method.
        rejectRowTheDatabaseRefuses(name, () -> magicRepository.saveAndFlush(magic));
    }

    public void updateMagic(String currentName, String newName, String castType, String accessType, boolean secondary) {
        String storedCastType = MagicCastType.requireStoredValue(castType);
        String storedAccessType = MagicAccessType.storableValueOrDefault(accessType);

        if (secondary) {
            rejectRowTheDatabaseRefuses(
                    newName,
                    () -> secondaryAdminDataService.orElseThrow()
                            .updateMagic(currentName, newName, storedCastType, storedAccessType)
            );
            return;
        }

        Magic magic = magicRepository.findByName(currentName)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + currentName));
        applyAndFlush(magic, newName, storedCastType, storedAccessType);
    }

    private void applyAndFlush(Magic magic, String name, String storedCastType, String storedAccessType) {
        magic.setName(name);
        magic.setCastType(storedCastType);
        magic.setAccessType(storedAccessType);
        rejectRowTheDatabaseRefuses(name, () -> magicRepository.saveAndFlush(magic));
    }

    /**
     * The database is the last word on both columns: cast_type carries a CHECK and access_type is a
     * NOT NULL varchar(10). Without this the page would show the raw SQL failure.
     */
    private void rejectRowTheDatabaseRefuses(String name, Runnable save) {
        try {
            save.run();
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            throw new IllegalArgumentException(
                    "The database rejected magic '" + name + "': "
                            + "cast type must be one of " + String.join(", ", MagicCastType.storedValues())
                            + " and access type must be 1 to 10 characters",
                    exception
            );
        }
    }

    public void removeMagic(String name, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().deleteMagic(name);
            return;
        }

        Magic magic = magicRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + name));
        magicRepository.delete(magic);
    }

    public void addCardToMagic(String magicName, String cardName, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().addCardToMagic(magicName, cardName);
            return;
        }

        Magic magic = magicRepository.findByName(magicName)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + magicName));
        Card card = cardRepository.findByName(cardName)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardName));
        MagicCard magicCard = new MagicCard(null, magic, card);
        magicCardRepository.save(magicCard);
        magic.addMagicCard(magicCard);
    }

    public void removeCardFromMagic(String magicName, String cardName, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().removeCardFromMagic(magicName, cardName);
            return;
        }

        Magic magic = magicRepository.findByName(magicName)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + magicName));
        Card card = cardRepository.findByName(cardName)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardName));
        magicCardRepository.deleteByMagicIdAndCardId(magic.getId(), card.getId());
    }

    @Transactional(readOnly = true)
    public List<MagicComparisonDto> getMagicComparisons() {
        Map<String, MagicDto> primaryMagicsByName = indexByName(
                getAllMagic(false),
                "Duplicate magic name in primary database"
        );
        Map<String, MagicDto> secondaryMagicsByName = secondaryAdminDataService
                .map(service -> indexByName(
                        service.getMagics(),
                        "Duplicate magic name in secondary database"
                ))
                .orElseGet(TreeMap::new);
        Set<String> names = new TreeSet<>(primaryMagicsByName.keySet());
        names.addAll(secondaryMagicsByName.keySet());

        return names.stream()
                .map(name -> {
                    MagicDto primaryMagic = primaryMagicsByName.get(name);
                    MagicDto secondaryMagic = secondaryMagicsByName.get(name);
                    Set<String> primaryCardNames = cardNamesOf(primaryMagic);
                    Set<String> secondaryCardNames = cardNamesOf(secondaryMagic);
                    Set<String> cardNames = new TreeSet<>(primaryCardNames);
                    cardNames.addAll(secondaryCardNames);
                    return new MagicComparisonDto(
                            name,
                            primaryMagic != null,
                            secondaryMagic != null,
                            primaryMagic == null ? null : primaryMagic.castType(),
                            secondaryMagic == null ? null : secondaryMagic.castType(),
                            primaryMagic == null ? null : primaryMagic.accessType(),
                            secondaryMagic == null ? null : secondaryMagic.accessType(),
                            cardNames.stream()
                                    .map(cardName -> new MagicCardComparisonDto(
                                            cardName,
                                            primaryCardNames.contains(cardName),
                                            secondaryCardNames.contains(cardName)
                                    ))
                                    .toList()
                    );
                })
                .toList();
    }

    private Map<String, MagicDto> indexByName(List<MagicDto> magics, String duplicateMessage) {
        return magics.stream()
                .collect(Collectors.toMap(
                        MagicDto::name,
                        magic -> magic,
                        (current, replacement) -> {
                            throw new IllegalStateException(duplicateMessage);
                        },
                        TreeMap::new
                ));
    }

    private Set<String> cardNamesOf(MagicDto magic) {
        if (magic == null) {
            return new TreeSet<>();
        }

        return magic.cardDtos().stream()
                .map(CardDto::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Transactional(readOnly = true)
    public List<String> getCardNames(boolean secondary) {
        return getAllCards(secondary).stream()
                .map(CardDto::name)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardDto> getAllCards() {
        return getAllCards(false);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getAllCards(boolean secondary) {
        if (secondary) {
            return secondaryAdminDataService.orElseThrow().getCards();
        }

        return cardRepository.findAll(org.springframework.data.domain.Sort.by("id"))
                .stream()
                .map(CardDto::new)
                .toList();
    }

    public SyncResult syncToSecondary() {
        return secondaryAdminDataService.orElseThrow().syncMagicsToSecondary(getAllMagic(false));
    }

    public SyncResult syncToPrimary() {
        List<MagicDto> magics = secondaryAdminDataService.orElseThrow().getMagics();
        List<String> changedNames = new java.util.ArrayList<>();
        int created = 0;
        int unchanged = 0;
        Map<String, Magic> existingMagicsByName = magicRepository.findAllBy().stream()
                .collect(Collectors.toMap(
                        Magic::getName,
                        magic -> magic,
                        (current, replacement) -> {
                            throw new IllegalStateException("Duplicate magic name in primary database");
                        }
                ));

        int updated = 0;

        for (MagicDto magic : magics) {
            Magic targetMagic = existingMagicsByName.get(magic.name());
            if (targetMagic == null) {
                targetMagic = new Magic();
                targetMagic.setName(magic.name());
                targetMagic.setCastType(magic.castType());
                targetMagic.setAccessType(magic.accessType());
                targetMagic = magicRepository.save(targetMagic);
                created++;
                changedNames.add(magic.name());
            } else if (!Objects.equals(targetMagic.getCastType(), magic.castType())
                    || !Objects.equals(targetMagic.getAccessType(), magic.accessType())) {
                targetMagic.setCastType(magic.castType());
                targetMagic.setAccessType(magic.accessType());
                updated++;
                changedNames.add(magic.name());
            } else {
                unchanged++;
            }

            magicCardRepository.deleteAll(magicCardRepository.findByMagicId(targetMagic.getId()));
            for (CardDto card : magic.cardDtos()) {
                Card targetCard = cardRepository.findByName(card.name())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Card not found in primary database: " + card.name()
                        ));
                magicCardRepository.save(new MagicCard(null, targetMagic, targetCard));
            }
        }

        return new SyncResult(
                created,
                updated,
                unchanged,
                changedNames
        );
    }
}
