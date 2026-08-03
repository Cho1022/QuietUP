package com.quietup.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageHistoryItemResponse(
        Long messageId,
        ChatMessageSenderRole senderRole,
        String content,
        LocalDateTime createdAt) {
}
