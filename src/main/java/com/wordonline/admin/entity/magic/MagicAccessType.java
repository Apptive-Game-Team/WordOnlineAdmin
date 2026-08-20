package com.wordonline.admin.entity.magic;

import java.util.List;

/**
 * What is actually known about {@code magics.access_type}.
 * <p>
 * Unlike {@link MagicCastType} this is not a closed set. The column is
 * {@code varchar(10) not null default 'DEFAULT'} with no CHECK constraint, no PostgreSQL enum and
 * no enum in any server or client repository. {@code DEFAULT} is the only value ever written to it,
 * and the only value any code reads: the lobby's {@code UserRepository} and this server's
 * {@code DefaultContentService} grant every magic whose {@code access_type} is {@code DEFAULT} to
 * every user. Any other value therefore just means "not granted by default".
 * <p>
 * Adventures use {@code FREE} / {@code PAID}, but that is a different column on a different table
 * and nothing proves those values mean anything for magics. So the known value is offered as a
 * suggestion rather than as a closed dropdown, and only the column's own limit is enforced.
 */
public final class MagicAccessType {

    public static final String DEFAULT = "DEFAULT";

    private static final int COLUMN_LENGTH = 10;

    private MagicAccessType() {
    }

    public static List<String> knownValues() {
        return List.of(DEFAULT);
    }

    public static String requireStorableValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(rejectionMessage(value));
        }

        String trimmed = value.trim();

        if (trimmed.length() > COLUMN_LENGTH) {
            throw new IllegalArgumentException(rejectionMessage(value));
        }

        return trimmed;
    }

    public static String rejectionMessage(String value) {
        return "Magic access type must be 1 to " + COLUMN_LENGTH + " characters"
                + " (" + DEFAULT + " grants the magic to every user); got: "
                + (value == null || value.isBlank() ? "(none)" : value);
    }
}
