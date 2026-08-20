package com.wordonline.admin.dto.counter;

public record MagicTagComparisonDto(
        String magicName,
        MagicTagDto primary,
        MagicTagDto secondary
) {
    public boolean primaryPresent() {
        return primary != null;
    }

    public boolean secondaryPresent() {
        return secondary != null;
    }

    public boolean untaggedSomewhere() {
        return (primaryPresent() && primary.untagged()) || (secondaryPresent() && secondary.untagged());
    }
}
