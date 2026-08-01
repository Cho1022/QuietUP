package com.quietup.global.error;

public class ResidenceAlreadyVerifiedException extends RuntimeException {

    public ResidenceAlreadyVerifiedException() {
        super("이미 거주 인증이 완료된 사용자입니다.");
    }
}
