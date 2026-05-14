package com.commerce.global.exception;

import org.springframework.http.HttpStatus;

public class ApiBadRequestException extends ApiException {
    public ApiBadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.BAD_REQUEST, message);
    }
}
