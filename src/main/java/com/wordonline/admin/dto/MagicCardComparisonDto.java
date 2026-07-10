package com.wordonline.admin.dto;

public record MagicCardComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent
) {
}
