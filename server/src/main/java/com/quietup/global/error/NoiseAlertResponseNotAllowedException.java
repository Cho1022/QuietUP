package com.quietup.global.error;

public class NoiseAlertResponseNotAllowedException extends RuntimeException {

    public NoiseAlertResponseNotAllowedException() {
        super("이 소음 알림에 응답할 수 없습니다.");
    }
}
