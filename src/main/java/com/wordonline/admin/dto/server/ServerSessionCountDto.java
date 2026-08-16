package com.wordonline.admin.dto.server;

/**
 * Number of sessions running on a game server. {@code sessionCount} is {@code null}
 * when the server did not answer. {@code database} disambiguates {@code serverId},
 * which is only unique within one database.
 */
public record ServerSessionCountDto(
        ServerDatabase database,
        Long serverId,
        Integer sessionCount
) {
}
