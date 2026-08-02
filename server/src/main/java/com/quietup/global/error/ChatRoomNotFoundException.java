package com.quietup.global.error;

public class ChatRoomNotFoundException extends RuntimeException {

    public ChatRoomNotFoundException() {
        super("채팅방을 찾을 수 없습니다.");
    }
}
