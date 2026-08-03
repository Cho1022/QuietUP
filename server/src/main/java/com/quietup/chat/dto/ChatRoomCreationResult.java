package com.quietup.chat.dto;

public record ChatRoomCreationResult(
        ChatRoomResponse chatRoom,
        boolean created) {
}
