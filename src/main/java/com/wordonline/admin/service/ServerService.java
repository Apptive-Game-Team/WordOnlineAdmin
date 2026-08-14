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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final GameServerClient gameServerClient;
    private final Optional<SecondaryServerService> secondaryServerService;

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

    private Stream<ServerSessionCountDto> sessionCounts(ServerDatabase database, List<ServerDto> servers) {
        return servers.stream()
                .filter(this::hasSessions)
                .map(server -> new ServerSessionCountDto(
                        database,
                        server.id(),
                        gameServerClient.getSessionCount(server.url())));
    }

    // A draining server still hosts the sessions it has not finished yet.
    private boolean hasSessions(ServerDto server) {
        return server.type() == ServerType.GAME && server.state() != ServerState.INACTIVE;
    }
}
