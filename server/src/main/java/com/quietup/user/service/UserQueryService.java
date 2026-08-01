package com.quietup.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quietup.global.error.UserNotFoundException;
import com.quietup.user.dto.CurrentUserResponse;
import com.quietup.user.entity.User;
import com.quietup.user.repository.UserRepository;

@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String subject) {
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new UserNotFoundException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
