package com.wordonline.admin.dto.counter;

import java.util.List;
import java.util.TreeSet;

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

    // The detach picker offers what the magic actually holds, so a tag only Dev carries can still
    // be taken off Dev.
    public List<String> attachedTagNames() {
        TreeSet<String> names = new TreeSet<>();
        if (primaryPresent()) names.addAll(primary.tagNames());
        if (secondaryPresent()) names.addAll(secondary.tagNames());
        return List.copyOf(names);
    }

    public boolean untaggedSomewhere() {
        return (primaryPresent() && primary.untagged()) || (secondaryPresent() && secondary.untagged());
    }
}
