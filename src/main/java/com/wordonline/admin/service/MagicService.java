package com.wordonline.admin.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.CardDto;
import com.wordonline.admin.dto.MagicDto;

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
    @Qualifier("jdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

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
        List<Long> changedIds = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        java.util.Map<Long, String> existingNamesById = jdbcTemplate.query(
                "select id, name from magics",
                (rs, rowNum) -> java.util.Map.entry(rs.getLong("id"), rs.getString("name"))
        ).stream().collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue));

        for (MagicDto magic : magics) {
            String existingName = existingNamesById.get(magic.id());
            if (existingName == null) {
                jdbcTemplate.update("insert into magics (id, name) values (?, ?)", magic.id(), magic.name());
                created++;
                changedIds.add(magic.id());
            } else if (!existingName.equals(magic.name())) {
                jdbcTemplate.update("update magics set name = ? where id = ?", magic.name(), magic.id());
                updated++;
                changedIds.add(magic.id());
            } else {
                unchanged++;
            }

            jdbcTemplate.update("delete from magic_cards where magic_id = ?", magic.id());
            for (CardDto card : magic.cardDtos()) {
                jdbcTemplate.update("insert into magic_cards (magic_id, card_id) values (?, ?)", magic.id(), card.id());
            }
        }

        return new SyncResult(
                created,
                updated,
                unchanged,
                changedIds.stream().map(id -> "magic#" + id).toList()
        );
    }
}
