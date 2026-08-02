package com.quietup.global.error;

public class NoiseAlertAlreadyResolvedException extends RuntimeException {

    public NoiseAlertAlreadyResolvedException() {
        super("이미 해결 처리된 소음 알림입니다.");
    }
}
