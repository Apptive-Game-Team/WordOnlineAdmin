package com.wordonline.admin.dto.tag;

import com.wordonline.admin.entity.tag.Tag;

public record TagRequestDto(
        String name
) {

    public Tag toEntity() {
        return new Tag(null, this.name);
    }
}
