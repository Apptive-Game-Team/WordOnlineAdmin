package com.wordonline.admin.dto;

import java.util.List;

public record GameObjectDto(
        Long id,
        String name,
        List<ParameterValueDto> parameterValues
) {

}
