package com.wordonline.admin.dto;

import com.wordonline.admin.entity.statistic.GameSessionStatus;
import com.wordonline.admin.entity.statistic.GameType;

// One row of the admin game-sessions page. suspectedLost marks sessions still
// IN_PROGRESS long after start: the game server's watchdog reaps stuck loops within
// seconds, so an old IN_PROGRESS row means the hosting process itself is gone.
public record GameSessionRowDto(
        Long id,
        String sessionId,
        Long leftUserId,
        Long rightUserId,
        GameType gameType,
        GameSessionStatus status,
        String endReason,
        String endDetail,
        Long statisticGameId,
        String server,
        String serverVersion,
        String startedAt,
        String endedAt,
        long durationSeconds,
        boolean suspectedLost,
        String statusBadgeClass
) {
}
