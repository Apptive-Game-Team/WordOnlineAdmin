package com.wordonline.admin.dto.server;

/**
 * Request to set one game server's bot scheduler target override.
 * {@code targetBotSessions} is {@code null} to clear the override, so the game server
 * falls back to its configured default.
 */
public record TargetBotSessionsUpdateDto(
        ServerDatabase database,
        Integer targetBotSessions
) {
}
