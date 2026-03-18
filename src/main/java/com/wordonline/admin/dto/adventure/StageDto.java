package com.wordonline.admin.dto.adventure;

import java.util.List;

import com.wordonline.admin.entity.adventure.Stage;

public record StageDto(
        Long id,
        Long adventureId,
        List<ScenarioDto> scenarios
) {

    public StageDto(Stage stage, List<ScenarioDto> scenarios) {
        this(stage.getId(), stage.getAdventure().getId(), scenarios);
    }
}
