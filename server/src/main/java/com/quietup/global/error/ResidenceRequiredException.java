package com.quietup.global.error;

public class ResidenceRequiredException extends RuntimeException {

    public ResidenceRequiredException() {
        super("거주 인증이 필요합니다.");
    }
}
