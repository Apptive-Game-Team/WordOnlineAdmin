package com.wordonline.admin.dto.counter;

public record CounterRuleComparisonDto(
        String attackerTagName,
        String targetTagName,
        CounterRuleDto primary,
        CounterRuleDto secondary
) {
    public boolean primaryPresent() {
        return primary != null;
    }

    public boolean secondaryPresent() {
        return secondary != null;
    }

    public boolean weightsDiffer() {
        return primaryPresent() && secondaryPresent() && primary.weight() != secondary.weight();
    }
}
