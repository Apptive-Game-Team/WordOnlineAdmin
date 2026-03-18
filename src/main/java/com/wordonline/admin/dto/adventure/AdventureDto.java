package com.wordonline.admin.dto.adventure;

import java.util.List;

import com.wordonline.admin.entity.adventure.Adventure;

public record AdventureDto(
        Long id,
        String name,
        String accessType,
        List<StageDto> stages
) {

    public AdventureDto(Adventure adventure, List<StageDto> stages) {
        this(adventure.getId(), adventure.getName(), adventure.getAccessType(), stages);
    }
}
