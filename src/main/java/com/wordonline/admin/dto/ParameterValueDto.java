package com.wordonline.admin.dto;

public record ParameterValueDto(
        Long id,
        Long parameterId,
        String parameterName,
        Double value
) {
    public ParameterValueDto(Long id, Long parameterId, Double value) {
        this(id, parameterId, null, value);
    }
}
