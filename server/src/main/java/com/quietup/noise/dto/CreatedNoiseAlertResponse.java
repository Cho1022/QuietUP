package com.quietup.noise.dto;

import java.time.LocalDateTime;

import com.quietup.noise.entity.NoiseAlertStatus;
import com.quietup.noise.entity.NoiseDirection;
import com.quietup.noise.entity.NoiseType;

public record CreatedNoiseAlertResponse(
        Long noiseAlertId,
        String targetLabel,
        NoiseDirection direction,
        NoiseType noiseType,
        String displayMessage,
        NoiseAlertStatus status,
        LocalDateTime createdAt) {
}
