package com.quietup.residence.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidenceStatusResponse(
        String status,
        String apartmentName,
        String roadAddress,
        String buildingNumber,
        String unitNumber,
        LocalDateTime verifiedAt) {

    public static ResidenceStatusResponse unverified() {
        return new ResidenceStatusResponse("UNVERIFIED", null, null, null, null, null);
    }

    public static ResidenceStatusResponse verified(
            String apartmentName,
            String roadAddress,
            String buildingNumber,
            String unitNumber,
            LocalDateTime verifiedAt) {
        return new ResidenceStatusResponse(
                "VERIFIED",
                apartmentName,
                roadAddress,
                buildingNumber,
                unitNumber,
                verifiedAt);
    }
}
