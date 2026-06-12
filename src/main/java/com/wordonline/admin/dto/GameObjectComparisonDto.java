package com.wordonline.admin.dto;

public record GameObjectComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent
) {
}
