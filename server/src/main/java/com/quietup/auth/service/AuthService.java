package com.quietup.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.auth.dto.LoginRequest;
import com.quietup.auth.dto.RefreshTokenRequest;
import com.quietup.auth.dto.TokenResponse;
import com.quietup.auth.entity.RefreshToken;
import com.quietup.auth.repository.RefreshTokenRepository;
import com.quietup.global.error.InvalidCredentialsException;
import com.quietup.global.error.InvalidRefreshTokenException;
import com.quietup.global.security.JwtTokenService;
import com.quietup.global.security.JwtTokenService.IssuedAccessToken;
import com.quietup.user.entity.User;
import com.quietup.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user.getId());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return tokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(refreshTokenService.hash(request.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (currentToken.isRevoked() || currentToken.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        currentToken.revoke(now);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(currentToken.getUser());
        IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(currentToken.getUser().getId());
        return tokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository
                .findByTokenHashForUpdate(refreshTokenService.hash(request.refreshToken()))
                .ifPresent(token -> token.revoke(LocalDateTime.now(ZoneOffset.UTC)));
    }

    private TokenResponse tokenResponse(
            IssuedAccessToken accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken) {
        return new TokenResponse(
                "Bearer",
                accessToken.tokenValue(),
                accessToken.expiresInSeconds(),
                refreshToken.tokenValue());
    }
}
