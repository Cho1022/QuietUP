package com.quietup.residence.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.residence.dto.ResidenceStatusResponse;
import com.quietup.residence.dto.ResidenceVerificationRequest;
import com.quietup.residence.dto.ResidenceVerificationResponse;
import com.quietup.residence.service.ResidenceVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/residences")
public class ResidenceController {

    private final ResidenceVerificationService residenceVerificationService;

    public ResidenceController(ResidenceVerificationService residenceVerificationService) {
        this.residenceVerificationService = residenceVerificationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<ResidenceVerificationResponse> verify(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResidenceVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(residenceVerificationService.verify(jwt.getSubject(), request));
    }

    @GetMapping("/me")
    public ResidenceStatusResponse me(@AuthenticationPrincipal Jwt jwt) {
        return residenceVerificationService.getMyStatus(jwt.getSubject());
    }
}
