package com.quietup.noise.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.noise.dto.CreateNoiseAlertRequest;
import com.quietup.noise.dto.CreatedNoiseAlertResponse;
import com.quietup.noise.dto.NoiseAlertDetailResponse;
import com.quietup.noise.dto.ReceivedNoiseAlertResponse;
import com.quietup.noise.dto.SentNoiseAlertResponse;
import com.quietup.noise.service.NoiseAlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/noise-alerts")
public class NoiseAlertController {

    private final NoiseAlertService noiseAlertService;

    public NoiseAlertController(NoiseAlertService noiseAlertService) {
        this.noiseAlertService = noiseAlertService;
    }

    @PostMapping
    public ResponseEntity<CreatedNoiseAlertResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateNoiseAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noiseAlertService.create(jwt.getSubject(), request));
    }

    @GetMapping("/received")
    public List<ReceivedNoiseAlertResponse> received(@AuthenticationPrincipal Jwt jwt) {
        return noiseAlertService.getReceived(jwt.getSubject());
    }

    @GetMapping("/sent")
    public List<SentNoiseAlertResponse> sent(@AuthenticationPrincipal Jwt jwt) {
        return noiseAlertService.getSent(jwt.getSubject());
    }

    @GetMapping("/{noiseAlertId}")
    public NoiseAlertDetailResponse detail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long noiseAlertId) {
        return noiseAlertService.getDetail(jwt.getSubject(), noiseAlertId);
    }
}
