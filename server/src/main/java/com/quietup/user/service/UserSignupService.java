package com.quietup.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.global.error.DuplicateEmailException;
import com.quietup.user.dto.SignupRequest;
import com.quietup.user.dto.SignupResponse;
import com.quietup.user.entity.User;
import com.quietup.user.repository.UserRepository;

@Service
public class UserSignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSignupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname());

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
