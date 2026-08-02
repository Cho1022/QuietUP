package com.quietup.chat.dto;

import java.time.LocalDateTime;

import com.quietup.chat.entity.ChatRoomStatus;
import com.quietup.noise.entity.NoiseType;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        Long noiseAlertId,
        ChatRoomStatus status,
        String counterpartLabel,
        NoiseType noiseType,
        LocalDateTime openedAt,
        LocalDateTime closedAt) {
}
