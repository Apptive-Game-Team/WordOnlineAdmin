package com.wordonline.admin.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${word-online.jwt.path}")
    private String jwtFilePath;

    @Bean
    public RestClient.Builder builder() {
        return RestClient.builder().defaultHeader("Authorization", loadJwt());
    }

    private String loadJwt() {
        try {
            return Files.readString(Path.of(jwtFilePath)).trim();
        } catch (IOException e) {
            log.error("Failed to load jwt {}", jwtFilePath, e);
            throw new IllegalStateException("Failed to load JWT from " + jwtFilePath, e);
        }
    }
}
