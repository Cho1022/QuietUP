package com.quietup.global.error;

import java.util.List;

public record ValidationErrorResponse(String code, String message, List<FieldErrorResponse> fieldErrors) {
}
