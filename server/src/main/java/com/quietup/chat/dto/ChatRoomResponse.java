package com.quietup.chat.dto;

import java.time.LocalDateTime;

import com.quietup.chat.entity.ChatRoomStatus;

public record ChatRoomResponse(
        Long chatRoomId,
        Long noiseAlertId,
        ChatRoomStatus status,
        String counterpartLabel,
        LocalDateTime openedAt) {
}
