package com.quietup.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.quietup.auth.entity.RefreshToken;
import com.quietup.auth.repository.RefreshTokenRepository;
import com.quietup.global.security.JwtProperties;
import com.quietup.user.entity.User;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    IssuedRefreshToken issue(User user) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC)
                .plusDays(jwtProperties.refreshExpirationDays());

        refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt));
        return new IssuedRefreshToken(rawToken);
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    record IssuedRefreshToken(String tokenValue) {
    }
}
