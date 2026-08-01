package com.quietup.global.error;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();

        return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다."));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "DUPLICATE_EMAIL",
                exception.getMessage()));
    }
}
