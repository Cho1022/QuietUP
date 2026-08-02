package com.quietup.noise.dto;

import com.quietup.noise.entity.ResponseType;

import jakarta.validation.constraints.NotNull;

public record NoiseAlertResponseRequest(
        @NotNull(message = "응답 유형은 필수입니다.")
        ResponseType responseType) {
}
