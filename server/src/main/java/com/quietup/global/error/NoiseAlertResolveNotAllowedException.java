package com.quietup.global.error;

public class NoiseAlertResolveNotAllowedException extends RuntimeException {

    public NoiseAlertResolveNotAllowedException() {
        super("이 소음 알림을 해결 처리할 수 없습니다.");
    }
}
