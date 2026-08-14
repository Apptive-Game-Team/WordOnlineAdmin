package com.wordonline.admin.client;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.wordonline.admin.dto.server.SessionLengthDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

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
