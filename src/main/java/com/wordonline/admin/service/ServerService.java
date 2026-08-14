package com.wordonline.admin.service;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.dto.server.ServerSessionCountDto;
import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final GameServerClient gameServerClient;

    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    public List<ServerSessionCountDto> getGameServerSessionCounts() {
        return serverRepository.findAll().stream()
                .filter(this::hasSessions)
                .map(server -> new ServerSessionCountDto(
                        server.getId(),
                        gameServerClient.getSessionCount(server.getUrl())))
                .toList();
    }

    // A draining server still hosts the sessions it has not finished yet.
    private boolean hasSessions(Server server) {
        return server.getType() == ServerType.GAME && server.getState() != ServerState.INACTIVE;
    }
}
