package com.wordonline.admin.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.wordonline.admin.dto.GameSessionRowDto;
import com.wordonline.admin.entity.statistic.GameSessionStatus;
import com.wordonline.admin.entity.statistic.StatisticGameSession;
import com.wordonline.admin.repository.statistic.StatisticGameSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameSessionServiceTest {

    private final StatisticGameSessionRepository repository = mock(StatisticGameSessionRepository.class);
    private final GameSessionService service = new GameSessionService(repository);

    @Test
    void countByStatusZeroFillsEveryStatus() {
        List<StatisticGameSession> sessions = List.of(
                sessionWithStatus(GameSessionStatus.COMPLETED),
                sessionWithStatus(GameSessionStatus.COMPLETED),
                sessionWithStatus(GameSessionStatus.ABANDONED));
        when(repository.findByStartedAtAfterOrderByStartedAtDesc(any())).thenReturn(sessions);

        Map<String, Long> counts = service.countByStatus(Instant.now().minus(Duration.ofDays(7)));

        assertThat(counts)
                .containsEntry("IN_PROGRESS", 0L)
                .containsEntry("COMPLETED", 2L)
                .containsEntry("DRAW", 0L)
                .containsEntry("ABANDONED", 1L);
    }

    // An old IN_PROGRESS row cannot mean a running game: the watchdog reaps stuck loops
    // within seconds, so it means the hosting process died without cleaning up.
    @Test
    void oldInProgressSessionIsMarkedSuspectedLost() {
        StatisticGameSession session = sessionWithStatus(GameSessionStatus.IN_PROGRESS);
        when(session.getStartedAt()).thenReturn(Instant.now().minus(Duration.ofMinutes(10)));
        when(repository.findByStartedAtAfterOrderByStartedAtDesc(any())).thenReturn(List.of(session));

        List<GameSessionRowDto> rows = service.getSessions(null, Instant.now().minus(Duration.ofDays(7)));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).suspectedLost()).isTrue();
        assertThat(rows.get(0).durationSeconds()).isGreaterThanOrEqualTo(590);
        assertThat(rows.get(0).endedAt()).isNull();
    }

    @Test
    void freshInProgressAndFinishedSessionsAreNotSuspectedLost() {
        StatisticGameSession fresh = sessionWithStatus(GameSessionStatus.IN_PROGRESS);
        when(fresh.getStartedAt()).thenReturn(Instant.now().minusSeconds(30));

        StatisticGameSession finished = sessionWithStatus(GameSessionStatus.ABANDONED);
        when(finished.getStartedAt()).thenReturn(Instant.now().minus(Duration.ofHours(2)));
        when(finished.getEndedAt()).thenReturn(Instant.now().minus(Duration.ofHours(1)));

        when(repository.findByStartedAtAfterOrderByStartedAtDesc(any()))
                .thenReturn(List.of(fresh, finished));

        List<GameSessionRowDto> rows = service.getSessions(null, Instant.now().minus(Duration.ofDays(7)));

        assertThat(rows).extracting(GameSessionRowDto::suspectedLost).containsExactly(false, false);
        assertThat(rows.get(1).durationSeconds()).isBetween(3595L, 3605L);
    }

    private StatisticGameSession sessionWithStatus(GameSessionStatus status) {
        StatisticGameSession session = mock(StatisticGameSession.class);
        when(session.getStatus()).thenReturn(status);
        when(session.getStartedAt()).thenReturn(Instant.now());
        return session;
    }
}
