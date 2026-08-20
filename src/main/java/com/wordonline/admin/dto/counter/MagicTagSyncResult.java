package com.wordonline.admin.dto.counter;

public record MagicTagSyncResult(Integer primaryRows, Integer secondaryRows) {
    public String message() {
        return "sync_magic_tags_from_game_objects(): Deploy=%s, Dev=%s (rows added)"
                .formatted(describe(primaryRows), describe(secondaryRows));
    }

    private static String describe(Integer rows) {
        return rows == null ? "skipped" : rows.toString();
    }
}
