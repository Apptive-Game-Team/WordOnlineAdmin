package com.wordonline.admin.dto;

import java.util.List;
import java.util.Objects;

public record MagicComparisonDto(
        String name,
        boolean primaryPresent,
        boolean secondaryPresent,
        String primaryCastType,
        String secondaryCastType,
        String primaryAccessType,
        String secondaryAccessType,
        List<MagicCardComparisonDto> cards
) {

    /**
     * A magic that exists on both sides but casts differently plays differently while its card list
     * can still match, so the card table alone would show the two sides as identical.
     */
    public boolean castTypeDiffers() {
        return primaryPresent && secondaryPresent
                && !Objects.equals(primaryCastType, secondaryCastType);
    }

    public boolean accessTypeDiffers() {
        return primaryPresent && secondaryPresent
                && !Objects.equals(primaryAccessType, secondaryAccessType);
    }
}
