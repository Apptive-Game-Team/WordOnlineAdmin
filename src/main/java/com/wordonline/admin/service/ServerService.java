package com.wordonline.admin.service;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.dto.server.ServerDatabase;
import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
