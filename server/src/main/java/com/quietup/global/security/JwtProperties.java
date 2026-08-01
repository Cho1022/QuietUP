package com.quietup.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "quietup.jwt")
public record JwtProperties(
        @NotBlank String secretBase64,
        @Positive long accessExpirationSeconds,
        @Positive long refreshExpirationDays) {
}
