package com.wordonline.admin.client;

import java.time.Duration;
import java.util.List;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.wordonline.admin.dto.server.SessionLengthDto;
import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    private final ServerRepository serverRepository;
    private final RestClient.Builder builder;
    private RestClient restClient;

    @PostConstruct
    private void initRestClient() {
        // The dashboard renders synchronously, so an unreachable game server must fail fast.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        restClient = builder.clone().requestFactory(requestFactory).build();
    }

    /**
     * @return the number of sessions running on the given game server, or {@code null}
     *         when the server did not answer.
     */
    public Integer getSessionCount(String serverUrl) {
        try {
            SessionLengthDto sessionLength = restClient.get()
                    .uri(serverUrl + "/api/server/game-sessions/length")
                    .retrieve()
                    .body(SessionLengthDto.class);
            return sessionLength == null ? null : sessionLength.length();
        } catch (Exception e) {
            log.warn("Can't get session count from {}: {}", serverUrl, e.getMessage());
            return null;
        }
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
                 .toList().stream() // To ensure the stream is fully executed
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
