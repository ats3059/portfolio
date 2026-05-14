package com.commerce.global.exception;

import org.springframework.http.HttpStatus;

public class ApiConflictException extends ApiException {
    public ApiConflictException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, message);
    }
}
