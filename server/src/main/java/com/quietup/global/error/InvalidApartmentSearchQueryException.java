package com.quietup.global.error;

public class InvalidApartmentSearchQueryException extends RuntimeException {

    public InvalidApartmentSearchQueryException() {
        super("검색어는 2자 이상이어야 합니다.");
    }
}
