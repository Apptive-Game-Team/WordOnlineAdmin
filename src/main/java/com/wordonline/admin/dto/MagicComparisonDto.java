package com.wordonline.admin.dto;

import java.util.List;

public record MagicComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent,
        List<MagicCardComparisonDto> cards
) {
}
