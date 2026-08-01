package com.quietup.auth.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken) {
}
