package com.wordonline.admin.dto;

public record ParameterComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent
) {
}
