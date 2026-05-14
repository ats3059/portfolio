package com.commerce.global.response;

public record ReturnResult<T>(
        String code,
        String message,
        T data
) {

    public static <T> ReturnResult<T> ok(T data) {
        return new ReturnResult<>("OK", "성공", data);
    }

    public static ReturnResult<Void> ok() {
        return new ReturnResult<>("OK", "성공", null);
    }

    public static <T> ReturnResult<T> created(T data) {
        return new ReturnResult<>("CREATED", "생성 완료", data);
    }
}
