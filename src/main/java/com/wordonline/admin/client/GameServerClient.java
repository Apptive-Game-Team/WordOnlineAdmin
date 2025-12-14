package com.wordonline.admin.client;

import java.util.List;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerClient {

    private final ServerRepository serverRepository;
    private final RestClient.Builder builder;
    private RestClient restClient;

    @PostConstruct
    private void initRestClient() {
        restClient = builder.build();
    }

    public void invalidateCache() {
        List<String> serverUrls = serverRepository
                .findAllByTypeAndState(ServerType.GAME, ServerState.ACTIVE)
                .stream()
                .map(Server::getUrl)
                .toList();

        boolean hasError = serverUrls.stream()
                 .map(this::mapToResponseEntity)
                 .map(ResponseEntity::getStatusCode)
                 .toList().stream() // 끝까지 실행 시키기 위함
                 .anyMatch(HttpStatusCode::isError);

        if (hasError) {
            log.error("Can't Invalidate Cache");
        }
    }

    private ResponseEntity<Void> mapToResponseEntity(String url) {
        return restClient.post()
                .uri(url + "/api/admin/invalidate")
                .retrieve()
                .toEntity(Void.class);
    }


}
