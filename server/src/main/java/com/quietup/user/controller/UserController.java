package com.quietup.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quietup.user.dto.CurrentUserResponse;
import com.quietup.user.service.UserQueryService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userQueryService.getCurrentUser(jwt.getSubject());
    }
}
