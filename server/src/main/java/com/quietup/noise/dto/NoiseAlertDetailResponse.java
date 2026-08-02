package com.quietup.noise.dto;

import java.time.LocalDateTime;

import com.quietup.noise.entity.NoiseAlertStatus;
import com.quietup.noise.entity.NoiseDirection;
import com.quietup.noise.entity.NoiseType;
import com.quietup.noise.entity.ResponseType;

public record NoiseAlertDetailResponse(
        Long noiseAlertId,
        String counterpartLabel,
        NoiseDirection direction,
        NoiseType noiseType,
        String displayMessage,
        NoiseAlertStatus status,
        ResponseType responseType,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        LocalDateTime resolvedAt) {
}
