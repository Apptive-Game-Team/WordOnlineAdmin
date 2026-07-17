package com.wordonline.admin.dto.bot;

import java.util.List;

public record BotAdminDto(
        long userId,
        String name,
        String tier,
        int thinkingTimeMs,
        int reactionIntervalFrames,
        double counterAggression,
        boolean enabled,
        short mmr,
        String status,
        Long selectedDeckId,
        String selectedDeckName,
        List<BotDeckCardDto> cards
) {
}
