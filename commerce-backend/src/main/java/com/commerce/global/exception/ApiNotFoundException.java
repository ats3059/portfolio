package com.commerce.global.exception;

import org.springframework.http.HttpStatus;

public class ApiNotFoundException extends ApiException {
    public ApiNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, message);
    }
}
