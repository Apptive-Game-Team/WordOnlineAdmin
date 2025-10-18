package com.wordonline.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
public class MagicService {

    private final MagicRepository magicRepository;
    private final CardRepository cardRepository;
    private final MagicCardRepository magicCardRepository;

    public List<MagicDto> getAllMagic() {
        return magicRepository.findAll()
                .stream()
                .map(MagicDto::new)
                .toList();
    }

    public void addCardToMagic(long magicId, long cardId) {
        Magic magic = magicRepository.findById(magicId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        MagicCard magicCard = new MagicCard(null, magic, card);

        magicCardRepository.save(magicCard);
        magic.addMagicCard(magicCard);
    }

    public void removeCardFromMagic(long magicId, long cardId) {
        Magic magic = magicRepository.findById(magicId)
                .orElseThrow(() -> new IllegalArgumentException("Magic Not Found"));

        MagicCard magicCard = magic.removeOneMagicCard(cardId);

        if (magicCard == null) {
            return;
        }

        magicCardRepository.delete(magicCard);
    }

    public void removeMagic(long magicId) {
        magicRepository.deleteById(magicId);
    }

    public void updateMagicName(long magicId, String name) {
        magicRepository.findById(magicId)
                .ifPresent(magic -> magic.setName(name));
    }

    public void createMagic(String name) {
        Magic magic = new Magic();
        magic.setName(name);
        magicRepository.save(magic);
    }

    public List<CardDto> getAllCards() {
        return cardRepository.findAll()
                .stream()
                .map(CardDto::new)
                .toList();
    }
}
