package com.quietup.residence.dto;

import java.time.LocalDateTime;

public record ResidenceVerificationResponse(
        String status,
        String apartmentName,
        String buildingNumber,
        String unitNumber,
        LocalDateTime verifiedAt) {
}
