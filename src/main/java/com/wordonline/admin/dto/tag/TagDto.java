package com.wordonline.admin.dto.tag;

import com.wordonline.admin.entity.tag.Tag;

public record TagDto(
        long id,
        String name
) {

    public TagDto(Tag tag) {
        this(tag.getId(), tag.getName());
    }

    public Tag toEntity() {
        return new Tag(id, name);
    }
}
