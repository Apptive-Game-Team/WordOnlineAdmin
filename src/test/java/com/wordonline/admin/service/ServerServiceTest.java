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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepository;
    @Mock
    private GameServerClient gameServerClient;
    @Mock
    private SecondaryServerService secondaryServerService;
    private ServerService serverService;

    @BeforeEach
    void setUp() {
        serverService = new ServerService(serverRepository, gameServerClient, Optional.empty());
    }

    @Test
    void getGameServerSessionCountsQueriesOnlyGameServersThatCanHoldSessions() {
        Server activeGame = new Server(1L, "http", "game-active", 8080, ServerType.GAME, ServerState.ACTIVE);
        Server drainingGame = new Server(2L, "http", "game-draining", 8080, ServerType.GAME, ServerState.DRAINING);
        Server inactiveGame = new Server(3L, "http", "game-inactive", 8080, ServerType.GAME, ServerState.INACTIVE);
        Server lobby = new Server(4L, "http", "lobby", 8080, ServerType.LOBBY, ServerState.ACTIVE);
        when(serverRepository.findAll()).thenReturn(List.of(activeGame, drainingGame, inactiveGame, lobby));
        when(gameServerClient.getSessionCount(activeGame.getUrl())).thenReturn(7);
        when(gameServerClient.getSessionCount(drainingGame.getUrl())).thenReturn(1);

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
    void invalidateGameServerCachesCallsEveryGameServerOfBothDatabases() {
        Server activeGame = new Server(1L, "http", "game-active", 8080, ServerType.GAME, ServerState.ACTIVE);
        Server drainingGame = new Server(2L, "http", "game-draining", 8080, ServerType.GAME, ServerState.DRAINING);
        Server inactiveGame = new Server(3L, "http", "game-inactive", 8080, ServerType.GAME, ServerState.INACTIVE);
        Server lobby = new Server(4L, "http", "lobby", 8080, ServerType.LOBBY, ServerState.ACTIVE);
        ServerDto devGame = new ServerDto(1L, "http", "dev-game", 8080, ServerType.GAME, ServerState.ACTIVE);
        serverService = new ServerService(serverRepository, gameServerClient, Optional.of(secondaryServerService));
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
    void getGameServerSessionCountsKeepsUnreachableServersWithoutACount() {
        Server activeGame = new Server(1L, "http", "game-active", 8080, ServerType.GAME, ServerState.ACTIVE);
        when(serverRepository.findAll()).thenReturn(List.of(activeGame));
        when(gameServerClient.getSessionCount(activeGame.getUrl())).thenReturn(null);

        List<ServerSessionCountDto> counts = serverService.getGameServerSessionCounts();

        assertEquals(List.of(new ServerSessionCountDto(ServerDatabase.PRIMARY, 1L, null)), counts);
    }
}
