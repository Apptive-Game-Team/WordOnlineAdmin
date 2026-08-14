package com.wordonline.admin.dto.server;

/**
 * Number of sessions running on a game server. {@code sessionCount} is {@code null}
 * when the server did not answer.
 */
public record ServerSessionCountDto(
        Long serverId,
        Integer sessionCount
) {
}
