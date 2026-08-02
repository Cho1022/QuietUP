package com.quietup.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        ChatMessageSenderRole senderRole,
        String content,
        LocalDateTime createdAt) {
}
