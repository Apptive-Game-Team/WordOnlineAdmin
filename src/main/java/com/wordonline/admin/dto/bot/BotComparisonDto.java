package com.wordonline.admin.dto.bot;

public record BotComparisonDto(
        long userId,
        BotAdminDto primary,
        BotAdminDto secondary
) {
    public boolean primaryPresent() {
        return primary != null;
    }

    public boolean secondaryPresent() {
        return secondary != null;
    }
}
