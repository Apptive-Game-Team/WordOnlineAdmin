package com.wordonline.admin.client;

import com.wordonline.admin.dto.auth.LoginRequestDto;
import com.wordonline.admin.dto.auth.TokenResponseDto;
import com.wordonline.admin.dto.auth.UserInfoDto;
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
        Server accountServer = serverRepository.findByType(ServerType.ACCOUNT)
                .orElseThrow(() -> new RuntimeException(
                        "Account server not found in database. Please ensure a server with type 'ACCOUNT' is configured."));
        restClient = builder.baseUrl(accountServer.getUrl())
                .build();
        log.info("Account server client initialized with URL: {}", accountServer.getUrl());
    }

    public String login(String username, String password) {
        if (restClient == null) {
            init();
        }

        LoginRequestDto loginRequest = new LoginRequestDto(username, password);
        
        try {
            TokenResponseDto response = restClient.post()
                    .uri("/api/auth/login")
                    .body(loginRequest)
                    .retrieve()
                    .body(TokenResponseDto.class);

            if (response == null || response.getToken() == null) {
                log.error("Login failed for user: {} - empty response", username);
                throw new RuntimeException("Login failed - empty response from server");
            }

            log.info("Login successful for user: {}", username);
            return response.getToken();
        } catch (Exception e) {
            log.error("Login failed for user: {}", username, e);
            throw new RuntimeException("Invalid username or password");
        }
    }

    public boolean validateToken(String token) {
        if (restClient == null) {
            init();
        }

        try {
            restClient.get()
                    .uri("/api/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return false;
        }
    }

    public UserInfoDto getUserInfo(String token) {
        if (restClient == null) {
            init();
        }

        try {
            UserInfoDto userInfo = restClient.get()
                    .uri("/api/auth/userinfo")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(UserInfoDto.class);

            return userInfo;
        } catch (Exception e) {
            log.debug("Failed to get user info", e);
            return null;
        }
    }
}
