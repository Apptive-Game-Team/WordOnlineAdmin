package com.wordonline.admin.dto.counter;

public record CounterRuleDto(
        Long id,
        String attackerTagName,
        String targetTagName,
        double weight
) {
    // Ids differ between the deploy and dev databases, so every cross-database comparison
    // and sync keys on the tag name pair instead.
    public String pairKey() {
        return attackerTagName + " -> " + targetTagName;
    }
}
