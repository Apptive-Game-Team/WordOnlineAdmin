package com.wordonline.admin.dto.adventure;

import com.wordonline.admin.entity.adventure.Scenario;

public record ScenarioDto(
        Long id,
        Long stageId
) {

    public ScenarioDto(Scenario scenario) {
        this(scenario.getId(), scenario.getStage().getId());
    }
}
