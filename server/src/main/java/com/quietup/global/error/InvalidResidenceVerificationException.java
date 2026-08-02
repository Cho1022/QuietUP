package com.quietup.global.error;

public class InvalidResidenceVerificationException extends RuntimeException {

    public InvalidResidenceVerificationException() {
        super("거주 인증 정보를 확인할 수 없습니다.");
    }
}
