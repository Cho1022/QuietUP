package com.quietup.global.error;

public class ChatRequestRequiredException extends RuntimeException {

    public ChatRequestRequiredException() {
        super("대화를 요청한 정형 응답이 필요합니다.");
    }
}
