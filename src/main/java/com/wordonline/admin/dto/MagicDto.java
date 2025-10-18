package com.wordonline.admin.dto;

import java.util.List;

import com.wordonline.admin.entity.magic.Magic;

public record MagicDto(
        long id,
        String name,
        List<CardDto> cardDtos
) {

    public MagicDto(Magic magic) {
        this(
                magic.getId(),
                magic.getName(),
                magic.getMagicCards()
                        .stream()
                        .map(CardDto::new)
                        .toList()
        );
    }
}

