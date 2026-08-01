package com.quietup.user.dto;

public record CurrentUserResponse(Long userId, String email, String nickname) {
}
