package com.commerce.global.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class Response {

    private Response() {
    }

    public static <T> ResponseEntity<ReturnResult<T>> ok(T data) {
        return ResponseEntity.ok(ReturnResult.ok(data));
    }

    public static ResponseEntity<ReturnResult<Void>> ok() {
        return ResponseEntity.ok(ReturnResult.ok());
    }

    public static <T> ResponseEntity<ReturnResult<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReturnResult.created(data));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
