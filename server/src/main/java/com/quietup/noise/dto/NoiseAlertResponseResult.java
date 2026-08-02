package com.quietup.noise.dto;

import java.time.LocalDateTime;

import com.quietup.noise.entity.NoiseAlertStatus;
import com.quietup.noise.entity.ResponseType;

public record NoiseAlertResponseResult(
        Long noiseAlertId,
        ResponseType responseType,
        NoiseAlertStatus status,
        LocalDateTime respondedAt) {
}
