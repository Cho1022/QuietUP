package com.quietup.global.error;

public class NoiseAlertNotFoundException extends RuntimeException {

    public NoiseAlertNotFoundException() {
        super("소음 알림을 찾을 수 없습니다.");
    }
}
