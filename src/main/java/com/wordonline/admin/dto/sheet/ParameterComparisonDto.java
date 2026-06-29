package com.wordonline.admin.dto.sheet;

public record ParameterComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent,
        Double primaryValue,
        Double secondaryValue
) {
}
