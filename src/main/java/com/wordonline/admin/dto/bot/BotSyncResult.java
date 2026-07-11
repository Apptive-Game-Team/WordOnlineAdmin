package com.wordonline.admin.dto.bot;

import java.util.List;

public record BotSyncResult(int created, int updated, List<Long> userIds) {
    public String message(String direction) {
        return "%s: created=%d, updated=%d, bots=%s".formatted(direction, created, updated, userIds);
    }
}
