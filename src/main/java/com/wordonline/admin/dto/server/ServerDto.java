package com.wordonline.admin.dto.server;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;

/**
 * A row of the {@code servers} table, read from either the primary or the secondary database.
 * The secondary database is not mapped by JPA, so the dashboard renders this instead of the
 * {@link Server} entity.
 */
public record ServerDto(
        Long id,
        String protocol,
        String domain,
        Integer port,
        ServerType type,
        ServerState state
) {

    public static ServerDto from(Server server) {
        return new ServerDto(
                server.getId(),
                server.getProtocol(),
                server.getDomain(),
                server.getPort(),
                server.getType(),
                server.getState()
        );
    }

    public String url() {
        return String.format("%s://%s:%d", protocol, domain, port);
    }
}
