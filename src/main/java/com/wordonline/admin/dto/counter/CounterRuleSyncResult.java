package com.wordonline.admin.dto.counter;

import java.util.List;

public record CounterRuleSyncResult(int created, int updated, List<String> pairs) {
    public String message(String direction) {
        return "%s: created=%d, updated=%d, rules=%s".formatted(direction, created, updated, pairs);
    }
}
