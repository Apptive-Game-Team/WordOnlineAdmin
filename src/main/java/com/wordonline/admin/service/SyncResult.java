package com.wordonline.admin.service;

import java.util.List;

public record SyncResult(
        int created,
        int updated,
        int unchanged,
        List<String> changedItems
) {
    public static SyncResult empty() {
        return new SyncResult(0, 0, 0, List.of());
    }

    public String toMessage(String title) {
        String details = changedItems.isEmpty()
                ? ""
                : "\nChanged: " + String.join(", ", changedItems.stream().limit(20).toList())
                + (changedItems.size() > 20 ? " ..." : "");

        return title + "\nCreated: " + created
                + "\nUpdated: " + updated
                + "\nUnchanged: " + unchanged
                + details;
    }
}
