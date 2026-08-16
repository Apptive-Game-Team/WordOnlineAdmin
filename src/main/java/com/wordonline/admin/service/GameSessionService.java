package com.wordonline.admin.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.wordonline.admin.dto.GameSessionRowDto;
import com.wordonline.admin.entity.statistic.GameSessionStatus;
import com.wordonline.admin.entity.statistic.StatisticGameSession;
import com.wordonline.admin.repository.statistic.StatisticGameSessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    // The game server's watchdog resolves stuck sessions within seconds, so any session
    // still IN_PROGRESS after this much time can only mean its process died uncleanly
    // and its startup sweep has not run yet (or the deployment is gone entirely).
    private static final Duration SUSPECTED_LOST_AFTER = Duration.ofMinutes(5);

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final StatisticGameSessionRepository statisticGameSessionRepository;

    public List<GameSessionRowDto> getSessions(GameSessionStatus status, Instant from) {
        return findSessions(status, from).stream()
                .map(this::toRow)
                .toList();
    }

    // Keyed by status name so the template can index it with a plain string.
    public Map<String, Long> countByStatus(Instant from) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (GameSessionStatus status : GameSessionStatus.values()) {
            counts.put(status.name(), 0L);
        }
        statisticGameSessionRepository.findByStartedAtAfterOrderByStartedAtDesc(from)
                .forEach(session -> counts.merge(session.getStatus().name(), 1L, Long::sum));
        return counts;
    }

    private List<StatisticGameSession> findSessions(GameSessionStatus status, Instant from) {
        if (status == null) {
            return statisticGameSessionRepository.findByStartedAtAfterOrderByStartedAtDesc(from);
        }
        return statisticGameSessionRepository.findByStatusAndStartedAtAfterOrderByStartedAtDesc(status, from);
    }

    private GameSessionRowDto toRow(StatisticGameSession session) {
        boolean inProgress = session.getStatus() == GameSessionStatus.IN_PROGRESS;
        Instant end = session.getEndedAt() != null ? session.getEndedAt() : Instant.now();
        long durationSeconds = Duration.between(session.getStartedAt(), end).toSeconds();
        boolean suspectedLost = inProgress
                && session.getStartedAt().isBefore(Instant.now().minus(SUSPECTED_LOST_AFTER));

        return new GameSessionRowDto(
                session.getId(),
                session.getSessionId(),
                session.getLeftUserId(),
                session.getRightUserId(),
                session.getGameType(),
                session.getStatus(),
                session.getEndReason(),
                session.getEndDetail(),
                session.getStatisticGameId(),
                session.getServerDomain() + ":" + session.getServerPort(),
                session.getServerVersion(),
                TIMESTAMP_FORMAT.format(session.getStartedAt()),
                session.getEndedAt() != null ? TIMESTAMP_FORMAT.format(session.getEndedAt()) : null,
                durationSeconds,
                suspectedLost,
                badgeClass(session.getStatus()));
    }

    private String badgeClass(GameSessionStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "badge badge-primary bg-primary";
            case COMPLETED -> "badge badge-success bg-success";
            case DRAW -> "badge badge-secondary bg-secondary";
            case ABANDONED -> "badge badge-danger bg-danger";
        };
    }
}
