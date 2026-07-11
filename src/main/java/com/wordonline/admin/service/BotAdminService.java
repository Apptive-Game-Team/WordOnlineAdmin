package com.wordonline.admin.service;

import com.wordonline.admin.dto.bot.BotAdminDto;
import com.wordonline.admin.dto.bot.BotDeckForm;
import com.wordonline.admin.dto.bot.BotForm;
import com.wordonline.admin.repository.bot.BotAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BotAdminService {

    private final BotAdminRepository repository;

    @Transactional(readOnly = true)
    public List<BotAdminDto> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public BotAdminDto find(long userId) {
        requireBotId(userId);
        return repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + userId));
    }

    public long create(BotForm form) {
        validate(form);
        long userId = repository.allocateUserId();
        requireBotId(userId);
        repository.createUser(userId, form);
        long deckId = repository.createDeck(userId, requireText(form.getDeckName(), "Deck name"));
        repository.selectDeck(userId, deckId);
        repository.createPersona(userId, form);
        return userId;
    }

    public void update(long userId, BotForm form) {
        requireBotId(userId);
        validate(form);
        repository.update(userId, form);
    }

    public void setEnabled(long userId, boolean enabled) {
        requireBotId(userId);
        repository.setEnabled(userId, enabled);
    }

    public void replaceDeck(long userId, BotDeckForm form) {
        requireBotId(userId);
        if (form.getCardIds().size() != form.getCounts().size()) {
            throw new IllegalArgumentException("Every card requires a count");
        }
        if (form.getCardIds().stream().distinct().count() != form.getCardIds().size()) {
            throw new IllegalArgumentException("Duplicate cards are not allowed");
        }
        if (form.getCounts().stream().anyMatch(count -> count == null || count < 1)) {
            throw new IllegalArgumentException("Card count must be positive");
        }
        repository.replaceSelectedDeckCards(userId, form.getCardIds(), form.getCounts());
    }

    public void delete(long userId) {
        requireBotId(userId);
        repository.setEnabled(userId, false);
        repository.delete(userId);
    }

    private void validate(BotForm form) {
        form.setName(requireText(form.getName(), "Name"));
        if (!List.of("INTRO", "BEGINNER", "INTERMEDIATE", "ADVANCED", "ELITE").contains(form.getTier())) {
            throw new IllegalArgumentException("Unsupported bot tier");
        }
        if (form.getThinkingTimeMs() < 0 || form.getReactionIntervalFrames() < 1) {
            throw new IllegalArgumentException("Invalid bot timing");
        }
        if (form.getCounterAggression() < 0 || form.getCounterAggression() > 1) {
            throw new IllegalArgumentException("Counter aggression must be between 0 and 1");
        }
        if (!List.of("Online", "OnMatching", "OnPlaying").contains(form.getStatus())) {
            throw new IllegalArgumentException("Unsupported user status");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private void requireBotId(long userId) {
        if (userId >= 0) throw new IllegalArgumentException("Bot user id must be negative");
    }
}
