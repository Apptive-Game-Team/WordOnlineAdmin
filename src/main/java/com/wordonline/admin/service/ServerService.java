package com.wordonline.admin.service;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.dto.server.ServerDatabase;
import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ServerService {

    /**
     * The game server heartbeats every 10s by default, so this tolerates two missed beats before
     * the dashboard stops trusting the number the server last wrote.
     */
    private static final Duration MAX_HEARTBEAT_AGE = Duration.ofSeconds(30);

    private final ServerRepository serverRepository;
    private final GameServerClient gameServerClient;
    private final Optional<SecondaryServerService> secondaryServerService;
    private final Clock clock;

    public List<ServerDto> getPrimaryServers() {
        return serverRepository.findAll().stream()
                .map(ServerDto::from)
                .toList();
    }

    public List<ServerDto> getSecondaryServers() {
        return secondaryServerService
                .map(SecondaryServerService::getServers)
                .orElseGet(List::of);
    }

    public List<ServerSessionCountDto> getGameServerSessionCounts() {
        return Stream.concat(
                sessionCounts(ServerDatabase.PRIMARY, getPrimaryServers()),
                sessionCounts(ServerDatabase.SECONDARY, getSecondaryServers())
        ).toList();
    }

    /**
     * Sets or clears ({@code null}) the bot scheduler target override of one game server.
     * The game server picks the new value up on its next scheduler tick; no restart needed.
     */
    public void updateTargetBotSessions(ServerDatabase database, long serverId, Integer targetBotSessions) {
        int updated = switch (database) {
            case PRIMARY -> serverRepository.updateTargetBotSessions(serverId, targetBotSessions);
            case SECONDARY -> updateSecondaryTargetBotSessions(serverId, targetBotSessions);
        };
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No game server with id " + serverId + " in the " + database + " database");
        }
    }

    private int updateSecondaryTargetBotSessions(long serverId, Integer targetBotSessions) {
        SecondaryServerService secondary = secondaryServerService.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secondary database is not configured"));
        try {
            return secondary.updateTargetBotSessions(serverId, targetBotSessions);
        } catch (DataAccessException e) {
            // Most likely the secondary database predates the target_bot_sessions column (V051).
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Secondary database rejected the update. Check the admin server log.", e);
        }
    }

    /**
     * Drops the parameter and magic caches on every reachable game server of both databases.
     *
     * @return the number of game servers that did not answer
     */
    public int invalidateGameServerCaches() {
        return (int) gameServers()
                .map(ServerDto::url)
                .distinct()
                .filter(url -> !gameServerClient.invalidateCache(url))
                .count();
    }

    private Stream<ServerDto> gameServers() {
        return Stream.concat(getPrimaryServers().stream(), getSecondaryServers().stream())
                .filter(this::hasSessions);
    }

    private Stream<ServerSessionCountDto> sessionCounts(ServerDatabase database, List<ServerDto> servers) {
        Instant now = clock.instant();
        return servers.stream()
                .filter(this::hasSessions)
                .map(server -> new ServerSessionCountDto(
                        database,
                        server.id(),
                        server.hasFreshHeartbeat(now, MAX_HEARTBEAT_AGE) ? server.sessionCount() : null));
    }

    // A draining server still hosts the sessions it has not finished yet.
    private boolean hasSessions(ServerDto server) {
        return server.type() == ServerType.GAME && server.state() != ServerState.INACTIVE;
    }
}
