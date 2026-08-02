package com.quietup.global.error;

public class ChatMessageInvalidException extends RuntimeException {

    public ChatMessageInvalidException() {
        super("채팅 메시지 요청이 올바르지 않습니다.");
    }
}
