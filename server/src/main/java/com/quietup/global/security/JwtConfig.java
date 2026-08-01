package com.quietup.global.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int MINIMUM_HS256_KEY_BYTES = 32;

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] decodedSecret;
        try {
            decodedSecret = Base64.getDecoder().decode(properties.secretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT 비밀키는 올바른 Base64 형식이어야 합니다.", exception);
        }

        if (decodedSecret.length < MINIMUM_HS256_KEY_BYTES) {
            throw new IllegalStateException("JWT 비밀키는 HS256에 필요한 최소 길이를 충족해야 합니다.");
        }

        return new SecretKeySpec(decodedSecret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
