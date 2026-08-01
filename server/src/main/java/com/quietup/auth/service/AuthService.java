package com.quietup.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.auth.dto.AccessTokenResponse;
import com.quietup.auth.dto.LoginRequest;
import com.quietup.global.error.InvalidCredentialsException;
import com.quietup.global.security.JwtTokenService;
import com.quietup.global.security.JwtTokenService.IssuedAccessToken;
import com.quietup.user.entity.User;
import com.quietup.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public AccessTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user.getId());
        return new AccessTokenResponse("Bearer", accessToken.tokenValue(), accessToken.expiresInSeconds());
    }
}
