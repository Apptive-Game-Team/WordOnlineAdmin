package com.wordonline.admin.dto;

public record ParameterRequestDto(
        String name,
        Boolean syncSecondary
) {
}
