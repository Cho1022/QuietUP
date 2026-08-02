package com.quietup.chat.dto;

import java.util.List;

public record ChatMessageHistoryResponse(
        List<ChatMessageHistoryItemResponse> messages,
        Long nextCursor) {
}
