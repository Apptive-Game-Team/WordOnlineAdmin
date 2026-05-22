package com.wordonline.admin.dto;

public record ParameterValueRequestDto(
        Long parameterId,
        Double value,
        Boolean syncSecondary
) {
}
