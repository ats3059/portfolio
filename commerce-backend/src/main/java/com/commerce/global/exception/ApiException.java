package com.commerce.global.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public abstract class ApiException extends ResponseStatusException {
    private final ApiErrorCode code;

    protected ApiException(HttpStatusCode status, ApiErrorCode code, String message) {
        super(status, message);
        this.code = code;
    }

    public ApiErrorCode getCode() {
        return code;
    }
}
