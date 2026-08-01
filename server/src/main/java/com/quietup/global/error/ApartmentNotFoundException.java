package com.quietup.global.error;

public class ApartmentNotFoundException extends RuntimeException {

    public ApartmentNotFoundException() {
        super("아파트 단지를 찾을 수 없습니다.");
    }
}
