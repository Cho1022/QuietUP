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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "INVALID_CREDENTIALS",
                exception.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                "INVALID_REFRESH_TOKEN",
                exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "USER_NOT_FOUND",
                exception.getMessage()));
    }

    @ExceptionHandler(ApartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApartmentNotFound(ApartmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "APARTMENT_NOT_FOUND",
                exception.getMessage()));
    }

    @ExceptionHandler(InvalidApartmentSearchQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidApartmentSearchQuery(
            InvalidApartmentSearchQueryException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_ERROR",
                exception.getMessage()));
    }

    @ExceptionHandler(ResidenceAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleResidenceAlreadyVerified(
            ResidenceAlreadyVerifiedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "RESIDENCE_ALREADY_VERIFIED",
                exception.getMessage()));
    }

    @ExceptionHandler(InvalidResidenceVerificationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResidenceVerification(
            InvalidResidenceVerificationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_RESIDENCE_VERIFICATION",
                exception.getMessage()));
    }

    @ExceptionHandler(ResidenceRequiredException.class)
    public ResponseEntity<ErrorResponse> handleResidenceRequired(ResidenceRequiredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "RESIDENCE_REQUIRED",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertTargetUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertTargetUnavailable(
            NoiseAlertTargetUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "NOISE_ALERT_TARGET_UNAVAILABLE",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertNotFound(NoiseAlertNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "NOISE_ALERT_NOT_FOUND",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertAlreadyRespondedException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertAlreadyResponded(
            NoiseAlertAlreadyRespondedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "NOISE_ALERT_ALREADY_RESPONDED",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertAlreadyResolvedException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertAlreadyResolved(
            NoiseAlertAlreadyResolvedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "NOISE_ALERT_ALREADY_RESOLVED",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertResponseNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertResponseNotAllowed(
            NoiseAlertResponseNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                "NOISE_ALERT_RESPONSE_NOT_ALLOWED",
                exception.getMessage()));
    }

    @ExceptionHandler(NoiseAlertResolveNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleNoiseAlertResolveNotAllowed(
            NoiseAlertResolveNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                "NOISE_ALERT_RESOLVE_NOT_ALLOWED",
                exception.getMessage()));
    }

    @ExceptionHandler(ChatRequestRequiredException.class)
    public ResponseEntity<ErrorResponse> handleChatRequestRequired(ChatRequestRequiredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "CHAT_REQUEST_REQUIRED",
                exception.getMessage()));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFound(ChatRoomNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                "CHAT_ROOM_NOT_FOUND",
                exception.getMessage()));
    }
}
