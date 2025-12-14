package com.wordonline.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class JwtConfig {

    @Value("${word-online.jwt.public-key}")
    private String publicKeyPath;

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAPublicKey publicKey = loadPublicKey(publicKeyPath);
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            log.error("Failed to load JWT public key from: {}", publicKeyPath, e);
            throw new RuntimeException("Failed to initialize JWT decoder with key: " + publicKeyPath, e);
        }
    }

    private RSAPublicKey loadPublicKey(String keyPath) throws Exception {
        String path = keyPath.startsWith("file:") ? keyPath.substring(5) : keyPath;
        
        if (path.startsWith("~")) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        
        String keyContent = Files.readString(Paths.get(path));
        
        keyContent = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}
