package com.wordonline.admin.dto.counter;

import java.util.List;

public record MagicTagDto(long magicId, String magicName, List<String> tagNames) {
    // An untagged magic never fails: BotCounterEvaluator returns the neutral score 0.0 and the
    // magic simply drops out of every bot's counter reasoning without an error anywhere.
    public boolean untagged() {
        return tagNames.isEmpty();
    }
}
