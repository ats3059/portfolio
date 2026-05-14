package com.commerce.global.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                ApiErrorCode.VALIDATION_ERROR.name(),
                "요청값이 올바르지 않습니다.",
                fieldErrors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldError(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                ApiErrorCode.VALIDATION_ERROR.name(),
                "요청값이 올바르지 않습니다.",
                fieldErrors
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiErrorResponse(
                exception.getCode().name(),
                exception.getReason(),
                null
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiErrorResponse(
                toErrorCode(exception.getStatusCode()).name(),
                exception.getReason() == null ? defaultMessage(exception.getStatusCode()) : exception.getReason(),
                null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                ApiErrorCode.INTERNAL_SERVER_ERROR.name(),
                "서버 내부 오류가 발생했습니다.",
                null
        ));
    }

    private ApiErrorResponse.FieldError toFieldError(FieldError fieldError) {
        return new ApiErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ApiErrorCode toErrorCode(HttpStatusCode statusCode) {
        int status = statusCode.value();
        if (status == 400) {
            return ApiErrorCode.BAD_REQUEST;
        }
        if (status == 404) {
            return ApiErrorCode.NOT_FOUND;
        }
        if (status == 409) {
            return ApiErrorCode.CONFLICT;
        }
        return ApiErrorCode.INTERNAL_SERVER_ERROR;
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "잘못된 요청입니다.";
            case 404 -> "요청한 대상을 찾을 수 없습니다.";
            case 409 -> "요청이 현재 상태와 충돌합니다.";
            default -> "서버 내부 오류가 발생했습니다.";
        };
    }
}
