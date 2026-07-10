package com.wordonline.admin.dto.sheet;

import java.util.List;

public record GameObjectComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent,
        List<ParameterComparisonDto> parameters
) {
}
