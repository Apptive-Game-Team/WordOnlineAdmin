package com.wordonline.admin.dto.counter;

// Magic ids are assigned per database, so the deploy and dev rows for one magic never share an id.
// The magic name is the only identifier both databases agree on, exactly as CounterRuleDto keys on
// the tag name pair.
public record MagicTagForm(String magicName, String tagName) {

    public MagicTagForm {
        magicName = requireText(magicName, "Magic");
        tagName = requireText(tagName, "Tag");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
