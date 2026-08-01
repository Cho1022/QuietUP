package com.quietup.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.user.dto.SignupRequest;
import com.quietup.user.dto.SignupResponse;
import com.quietup.user.service.UserSignupService;
import com.quietup.auth.dto.LoginRequest;
import com.quietup.auth.dto.RefreshTokenRequest;
import com.quietup.auth.dto.TokenResponse;
import com.quietup.auth.service.AuthService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserSignupService userSignupService;
    private final AuthService authService;

    public AuthController(UserSignupService userSignupService, AuthService authService) {
        this.userSignupService = userSignupService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userSignupService.signup(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
