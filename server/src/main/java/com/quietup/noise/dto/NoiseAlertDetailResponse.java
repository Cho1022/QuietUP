package com.quietup.noise.dto;

import java.time.LocalDateTime;

import com.quietup.noise.entity.NoiseAlertStatus;
import com.quietup.noise.entity.NoiseDirection;
import com.quietup.noise.entity.NoiseType;

public record NoiseAlertDetailResponse(
        Long noiseAlertId,
        String counterpartLabel,
        NoiseDirection direction,
        NoiseType noiseType,
        String displayMessage,
        NoiseAlertStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        LocalDateTime resolvedAt) {
}
