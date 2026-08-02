package com.quietup.global.error;

public class NoiseAlertTargetUnavailableException extends RuntimeException {

    public NoiseAlertTargetUnavailableException() {
        super("현재 알림을 전달할 수 있는 대상이 없습니다.");
    }
}
