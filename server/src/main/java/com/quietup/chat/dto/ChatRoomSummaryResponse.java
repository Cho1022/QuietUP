package com.quietup.chat.dto;

import java.time.LocalDateTime;

import com.quietup.chat.entity.ChatRoomStatus;

public record ChatRoomSummaryResponse(
        Long chatRoomId,
        Long noiseAlertId,
        ChatRoomStatus status,
        String counterpartLabel,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime openedAt) {
}
