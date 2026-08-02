package com.quietup.noise.dto;

import com.quietup.noise.entity.NoiseDirection;
import com.quietup.noise.entity.NoiseType;

import jakarta.validation.constraints.NotNull;

public record CreateNoiseAlertRequest(
        @NotNull(message = "알림 방향은 필수입니다.")
        NoiseDirection direction,
        @NotNull(message = "소음 유형은 필수입니다.")
        NoiseType noiseType) {
}
