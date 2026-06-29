package com.wordonline.admin.dto.sheet;

public record ParameterComparisonDto(
        String name,
        Double primaryValue,
        Double secondaryValue
) {
}
