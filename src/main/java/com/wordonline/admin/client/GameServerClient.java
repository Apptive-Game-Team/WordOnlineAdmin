package com.wordonline.admin.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GameServerClient {
    private final RestClient.Builder builder;
    private RestClient restClient;

    @Value("${word-online.game-server-url}")
    private String gameServerUrl;

    private void init() {
        restClient = builder.baseUrl(gameServerUrl)
                .build();
    }

    public void invalidateCache() {
        if (restClient == null) {
            init();
        }

        ResponseEntity<Void> response = restClient.post()
                .uri("/api/admin/invalidate")
                .retrieve()
                .toEntity(Void.class);

        if (response.getStatusCode().isError()) {
            log.error("Can't Invalidate Cache");
        }
    }

    private static final Logger log = LoggerFactory.getLogger(GameServerClient.class);
}
