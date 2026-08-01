package com.quietup.auth.dto;

public record AccessTokenResponse(String tokenType, String accessToken, long accessTokenExpiresIn) {
}
