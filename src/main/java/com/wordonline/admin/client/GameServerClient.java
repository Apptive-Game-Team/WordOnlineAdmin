package com.wordonline.admin.client;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    private final RestClient.Builder builder;
    private RestClient restClient;

    @PostConstruct
    private void initRestClient() {
        // An admin waits for this button, so an unreachable game server must fail fast.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        restClient = builder.clone().requestFactory(requestFactory).build();
    }

    /**
     * @return {@code true} when the given game server dropped its cache. One unreachable server
     *         must not stop the caller from reaching the rest, so failures are reported instead
     *         of thrown.
     */
    public boolean invalidateCache(String serverUrl) {
        try {
            restClient.post()
                    .uri(serverUrl + "/api/admin/invalidate")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Can't invalidate cache on {}: {}", serverUrl, e.getMessage());
            return false;
        }
    }
}
