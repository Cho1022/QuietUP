package com.quietup.global.error;

public class NoiseAlertAlreadyRespondedException extends RuntimeException {

    public NoiseAlertAlreadyRespondedException() {
        super("이미 응답이 완료된 소음 알림입니다.");
    }
}
