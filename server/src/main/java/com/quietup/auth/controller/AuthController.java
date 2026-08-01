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

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserSignupService userSignupService;

    public AuthController(UserSignupService userSignupService) {
        this.userSignupService = userSignupService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userSignupService.signup(request));
    }
}
