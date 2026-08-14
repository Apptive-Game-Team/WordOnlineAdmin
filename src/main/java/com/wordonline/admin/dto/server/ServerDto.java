package com.wordonline.admin.dto.server;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;

import java.time.Duration;
import java.time.Instant;

/**
 * A row of the {@code servers} table, read from either the primary or the secondary database.
 * The secondary database is not mapped by JPA, so the dashboard renders this instead of the
 * {@link Server} entity.
 *
 * <p>{@code sessionCount} and {@code lastHeartbeatAt} are written by the game server on every
 * heartbeat. A database that predates those columns reports both as {@code null}.
 */
public record ServerDto(
        Long id,
        String protocol,
        String domain,
        Integer port,
        ServerType type,
        ServerState state,
        Integer sessionCount,
        Instant lastHeartbeatAt
) {

    /** A row from a database that predates the heartbeat columns. */
    public ServerDto(Long id, String protocol, String domain, Integer port, ServerType type, ServerState state) {
        this(id, protocol, domain, port, type, state, null, null);
    }

    public static ServerDto from(Server server) {
        return new ServerDto(
                server.getId(),
                server.getProtocol(),
                server.getDomain(),
                server.getPort(),
                server.getType(),
                server.getState(),
                server.getSessionCount(),
                server.getLastHeartbeatAt()
        );
    }

    public String url() {
        return String.format("%s://%s:%d", protocol, domain, port);
    }

    /**
     * A server that stopped reporting keeps its last written {@code session_count} forever, so
     * the number only means something while the heartbeat is recent.
     */
    public boolean hasFreshHeartbeat(Instant now, Duration maxAge) {
        return lastHeartbeatAt != null && !lastHeartbeatAt.isBefore(now.minus(maxAge));
    }
}
