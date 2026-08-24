package com.wordonline.admin.entity.magic;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * The values the {@code CHECK (cast_type IN (...))} constraint on {@code magics} allows.
 * <p>
 * The column stores lowercase text and the lobby server reads it as a raw string
 * ({@code com.wordonline.matching.magic.domain.Magic#castType}), so the stored form is written out
 * here instead of being derived from the constant name.
 */
@Getter
public enum MagicCastType {

    SPAWN("spawn"),
    DROP("drop"),
    EXPLODE("explode"),
    BUILD("build"),
    SHOOT("shoot");

    private final String storedValue;

    MagicCastType(String storedValue) {
        this.storedValue = storedValue;
    }

    public static List<String> storedValues() {
        return Arrays.stream(values())
                .map(MagicCastType::getStoredValue)
                .toList();
    }

    public static String requireStoredValue(String value) {
        return storedValues().stream()
                .filter(storedValue -> storedValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(rejectionMessage(value)));
    }

    public static String rejectionMessage(String value) {
        return "Magic cast type must be one of " + String.join(", ", storedValues())
                + "; got: " + (value == null || value.isBlank() ? "(none)" : value);
    }
}
