package com.wordonline.admin.dto.adventure;

public record BulkCreateStagesRequestDto(
        int stageCount,
        int scenarioCount
) {
}
