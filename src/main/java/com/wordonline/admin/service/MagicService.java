package com.wordonline.admin.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.CardDto;
import com.wordonline.admin.dto.MagicDto;
import com.wordonline.admin.dto.MagicComparisonDto;
import com.wordonline.admin.dto.MagicCardComparisonDto;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.magic.MagicCard;
import com.wordonline.admin.repository.magic.CardRepository;
import com.wordonline.admin.repository.magic.MagicCardRepository;
import com.wordonline.admin.repository.magic.MagicRepository;

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

    public void updateMagicName(long magicId, String name) {
        updateMagicName(magicId, name, false);
    }

    public void updateMagicName(long magicId, String name, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().updateMagicName(magicId, name);
            return;
        }

        magicRepository.findById(magicId)
                .ifPresent(magic -> magic.setName(name));
    }

    public void createMagic(String name) {
        createMagic(name, false);
    }

    public void createMagic(String name, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().createMagic(name);
            return;
        }

        Magic magic = new Magic();
        magic.setName(name);
        magicRepository.save(magic);
    }

    public void updateMagicName(String currentName, String newName, boolean secondary) {
        if (secondary) {
            secondaryAdminDataService.orElseThrow().updateMagicName(currentName, newName);
            return;
        }

        Magic magic = magicRepository.findByName(currentName)
                .orElseThrow(() -> new IllegalArgumentException("Magic not found: " + currentName));
        magic.setName(newName);
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
        Map<String, List<String>> primaryCardsByMagic = getAllMagic(false).stream()
                .collect(Collectors.toMap(
                        MagicDto::name,
                        magic -> magic.cardDtos().stream()
                                .map(CardDto::name)
                                .sorted()
                                .toList(),
                        (current, replacement) -> {
                            throw new IllegalStateException("Duplicate magic name in primary database");
                        },
                        TreeMap::new
                ));
        Map<String, List<String>> secondaryCardsByMagic = secondaryAdminDataService
                .map(service -> service.getMagics().stream()
                        .collect(Collectors.toMap(
                                MagicDto::name,
                                magic -> magic.cardDtos().stream()
                                        .map(CardDto::name)
                                        .sorted()
                                        .toList(),
                                (current, replacement) -> {
                                    throw new IllegalStateException("Duplicate magic name in secondary database");
                                },
                                TreeMap::new
                        )))
                .orElseGet(TreeMap::new);
        Set<String> names = new TreeSet<>(primaryCardsByMagic.keySet());
        names.addAll(secondaryCardsByMagic.keySet());

        return names.stream()
                .map(name -> {
                    Set<String> primaryCardNames = new TreeSet<>(
                            primaryCardsByMagic.getOrDefault(name, List.of())
                    );
                    Set<String> secondaryCardNames = new TreeSet<>(
                            secondaryCardsByMagic.getOrDefault(name, List.of())
                    );
                    Set<String> cardNames = new TreeSet<>(primaryCardNames);
                    cardNames.addAll(secondaryCardNames);
                    return new MagicComparisonDto(
                            name,
                            primaryCardsByMagic.containsKey(name),
                            secondaryCardsByMagic.containsKey(name),
                            cardNames.stream()
                                    .filter(cardName ->
                                            primaryCardNames.contains(cardName)
                                                || secondaryCardNames.contains(cardName)
                                    )
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

        for (MagicDto magic : magics) {
            Magic targetMagic = existingMagicsByName.get(magic.name());
            if (targetMagic == null) {
                targetMagic = new Magic();
                targetMagic.setName(magic.name());
                targetMagic = magicRepository.save(targetMagic);
                created++;
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
                0,
                unchanged,
                changedNames
        );
    }
}
