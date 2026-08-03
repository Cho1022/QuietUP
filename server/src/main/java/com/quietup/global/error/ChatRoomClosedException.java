package com.quietup.global.error;

public class ChatRoomClosedException extends RuntimeException {

    public ChatRoomClosedException() {
        super("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
    }
}
