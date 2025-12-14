package com.wordonline.admin.client;

import com.wordonline.admin.dto.auth.LoginRequestDto;
import com.wordonline.admin.dto.auth.TokenResponseDto;
import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerType;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
        Server accountServer = serverRepository.findByType(ServerType.Account)
                .orElseThrow(() -> new RuntimeException("Account server not found"));
        restClient = builder.baseUrl(accountServer.getUrl())
                .build();
        log.info("Account server client initialized with URL: {}", accountServer.getUrl());
    }

    public String login(String username, String password) {
        if (restClient == null) {
            init();
        }

        LoginRequestDto loginRequest = new LoginRequestDto(username, password);
        
        ResponseEntity<TokenResponseDto> response = restClient.post()
                .uri("/api/auth/login")
                .body(loginRequest)
                .retrieve()
                .toEntity(TokenResponseDto.class);

        if (response.getStatusCode().isError() || response.getBody() == null) {
            log.error("Login failed for user: {}", username);
            throw new RuntimeException("Login failed");
        }

        log.info("Login successful for user: {}", username);
        return response.getBody().getToken();
    }

    public boolean validateToken(String token) {
        if (restClient == null) {
            init();
        }

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri("/api/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toEntity(Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
}
