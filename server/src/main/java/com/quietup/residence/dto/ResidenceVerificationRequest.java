package com.quietup.residence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResidenceVerificationRequest(
        @NotNull(message = "아파트 단지는 필수입니다.")
        Long apartmentId,
        @NotBlank(message = "동 번호는 필수입니다.")
        @Size(max = 20, message = "동 번호는 20자 이하여야 합니다.")
        String buildingNumber,
        @NotBlank(message = "호수는 필수입니다.")
        @Size(max = 20, message = "호수는 20자 이하여야 합니다.")
        String unitNumber,
        String verificationCode) {

    public ResidenceVerificationRequest {
        if (buildingNumber != null) {
            buildingNumber = buildingNumber.trim();
        }
        if (unitNumber != null) {
            unitNumber = unitNumber.trim();
        }
    }
}
