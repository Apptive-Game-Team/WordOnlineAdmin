package com.wordonline.admin.client;

import com.wordonline.admin.dto.auth.LoginRequestDto;
import com.wordonline.admin.dto.auth.TokenResponseDto;
import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServerClient {
    private final RestClient.Builder builder;
    private final ServerRepository serverRepository;
    private RestClient restClient;

    private void init() {
        Server accountServer = serverRepository.findAllByTypeAndState(ServerType.ACCOUNT, ServerState.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Account server not found in database. Please configure an ACTIVE ACCOUNT server."
                ));
        String url = accountServer.getInterServerUrl();
        restClient = builder.baseUrl(url)
                .build();
        log.info("Account server client initialized with URL: {}", url);
    }

    public String login(String username, String password) {
        if (restClient == null) {
            init();
        }

        LoginRequestDto loginRequest = new LoginRequestDto(username, password);
        
        try {
            TokenResponseDto response = restClient.post()
                    .uri("/api/members/login")
                    .body(loginRequest)
                    .retrieve()
                    .body(TokenResponseDto.class);

            if (response == null || response.getJwt() == null) {
                log.error("Login failed for user: {} - empty response", username);
                throw new IllegalStateException("Account server returned an empty login response");
            }

            log.info("Login successful for user: {}", username);
            return response.getJwt();
        } catch (Exception e) {
            log.error("Login failed for user: {}", username, e);
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new RuntimeException("Login request to account server failed", e);
        }
    }
}
