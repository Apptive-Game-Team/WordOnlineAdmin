package com.wordonline.admin.service;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.dto.server.ServerDatabase;
import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ServerRepository serverRepository;
    @Mock
    private GameServerClient gameServerClient;
    @Mock
    private SecondaryServerService secondaryServerService;
    private ServerService serverService;

    @BeforeEach
    void setUp() {
        serverService = new ServerService(serverRepository, gameServerClient, Optional.empty(), CLOCK);
    }

    @Test
    void getGameServerSessionCountsReadsTheHeartbeatOfEveryGameServerThatCanHoldSessions() {
        Server activeGame = heartbeatingGame(1L, "game-active", ServerState.ACTIVE, 7, NOW.minusSeconds(5));
        Server drainingGame = heartbeatingGame(2L, "game-draining", ServerState.DRAINING, 1, NOW.minusSeconds(5));
        Server inactiveGame = heartbeatingGame(3L, "game-inactive", ServerState.INACTIVE, 4, NOW.minusSeconds(5));
        Server lobby = new Server(4L, "http", "lobby", 8080, ServerType.LOBBY, ServerState.ACTIVE);
        when(serverRepository.findAll()).thenReturn(List.of(activeGame, drainingGame, inactiveGame, lobby));

        List<ServerSessionCountDto> counts = serverService.getGameServerSessionCounts();

        assertEquals(
                List.of(
                        new ServerSessionCountDto(ServerDatabase.PRIMARY, 1L, 7),
                        new ServerSessionCountDto(ServerDatabase.PRIMARY, 2L, 1)
                ),
                counts
        );
    }

    @Test
    void getGameServerSessionCountsDropsTheCountOfAServerThatStoppedHeartbeating() {
        Server stale = heartbeatingGame(1L, "game-stale", ServerState.ACTIVE, 3, NOW.minusSeconds(31));
        Server neverReported = new Server(2L, "http", "game-new", 8080, ServerType.GAME, ServerState.ACTIVE);
        when(serverRepository.findAll()).thenReturn(List.of(stale, neverReported));

        List<ServerSessionCountDto> counts = serverService.getGameServerSessionCounts();

        assertEquals(
                List.of(
                        new ServerSessionCountDto(ServerDatabase.PRIMARY, 1L, null),
                        new ServerSessionCountDto(ServerDatabase.PRIMARY, 2L, null)
                ),
                counts
        );
    }

    @Test
    void getGameServerSessionCountsCoversBothDatabases() {
        serverService = new ServerService(serverRepository, gameServerClient, Optional.of(secondaryServerService), CLOCK);
        when(serverRepository.findAll()).thenReturn(
                List.of(heartbeatingGame(1L, "game", ServerState.ACTIVE, 7, NOW.minusSeconds(5))));
        when(secondaryServerService.getServers()).thenReturn(List.of(new ServerDto(
                1L, "http", "dev-game", 8080, ServerType.GAME, ServerState.ACTIVE, 2, NOW.minusSeconds(5))));

        assertEquals(
                List.of(
                        new ServerSessionCountDto(ServerDatabase.PRIMARY, 1L, 7),
                        new ServerSessionCountDto(ServerDatabase.SECONDARY, 1L, 2)
                ),
                serverService.getGameServerSessionCounts()
        );
    }

    @Test
    void invalidateGameServerCachesCallsEveryGameServerOfBothDatabases() {
        Server activeGame = new Server(1L, "http", "game-active", 8080, ServerType.GAME, ServerState.ACTIVE);
        Server drainingGame = new Server(2L, "http", "game-draining", 8080, ServerType.GAME, ServerState.DRAINING);
        Server inactiveGame = new Server(3L, "http", "game-inactive", 8080, ServerType.GAME, ServerState.INACTIVE);
        Server lobby = new Server(4L, "http", "lobby", 8080, ServerType.LOBBY, ServerState.ACTIVE);
        ServerDto devGame = new ServerDto(
                1L, "http", "dev-game", 8080, ServerType.GAME, ServerState.ACTIVE, null, null);
        serverService = new ServerService(serverRepository, gameServerClient, Optional.of(secondaryServerService), CLOCK);
        when(serverRepository.findAll()).thenReturn(List.of(activeGame, drainingGame, inactiveGame, lobby));
        when(secondaryServerService.getServers()).thenReturn(List.of(devGame));
        when(gameServerClient.invalidateCache(anyString())).thenReturn(true);

        int failed = serverService.invalidateGameServerCaches();

        assertEquals(0, failed);
        verify(gameServerClient).invalidateCache(activeGame.getUrl());
        verify(gameServerClient).invalidateCache(drainingGame.getUrl());
        verify(gameServerClient).invalidateCache(devGame.url());
        verify(gameServerClient, never()).invalidateCache(inactiveGame.getUrl());
        verify(gameServerClient, never()).invalidateCache(lobby.getUrl());
    }

    @Test
    void invalidateGameServerCachesCountsTheServersThatDidNotAnswer() {
        Server reachable = new Server(1L, "http", "game-reachable", 8080, ServerType.GAME, ServerState.ACTIVE);
        Server unreachable = new Server(2L, "http", "game-unreachable", 8080, ServerType.GAME, ServerState.ACTIVE);
        when(serverRepository.findAll()).thenReturn(List.of(reachable, unreachable));
        when(gameServerClient.invalidateCache(reachable.getUrl())).thenReturn(true);
        when(gameServerClient.invalidateCache(unreachable.getUrl())).thenReturn(false);

        assertEquals(1, serverService.invalidateGameServerCaches());
    }

    @Test
    void updateTargetBotSessionsWritesThePrimaryRowThroughTheTargetedQuery() {
        when(serverRepository.updateTargetBotSessions(1L, 4)).thenReturn(1);

        serverService.updateTargetBotSessions(ServerDatabase.PRIMARY, 1L, 4);

        verify(serverRepository).updateTargetBotSessions(1L, 4);
    }

    @Test
    void updateTargetBotSessionsRejectsAnIdThatIsNotAGameServer() {
        when(serverRepository.updateTargetBotSessions(9L, 2)).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serverService.updateTargetBotSessions(ServerDatabase.PRIMARY, 9L, 2));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateTargetBotSessionsDelegatesSecondaryWritesToTheSecondaryService() {
        serverService = new ServerService(serverRepository, gameServerClient, Optional.of(secondaryServerService), CLOCK);
        when(secondaryServerService.updateTargetBotSessions(1L, null)).thenReturn(1);

        serverService.updateTargetBotSessions(ServerDatabase.SECONDARY, 1L, null);

        verify(secondaryServerService).updateTargetBotSessions(1L, null);
    }

    @Test
    void updateTargetBotSessionsFailsClearlyWithoutASecondaryDatabase() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serverService.updateTargetBotSessions(ServerDatabase.SECONDARY, 1L, 1));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateTargetBotSessionsSurfacesASecondarySchemaThatPredatesTheColumn() {
        serverService = new ServerService(serverRepository, gameServerClient, Optional.of(secondaryServerService), CLOCK);
        when(secondaryServerService.updateTargetBotSessions(1L, 1))
                .thenThrow(new DataIntegrityViolationException("no target_bot_sessions column"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serverService.updateTargetBotSessions(ServerDatabase.SECONDARY, 1L, 1));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    private Server heartbeatingGame(Long id, String domain, ServerState state, int sessionCount, Instant lastHeartbeatAt) {
        return new Server(id, "http", domain, 8080, ServerType.GAME, state, sessionCount, lastHeartbeatAt);
    }
}
