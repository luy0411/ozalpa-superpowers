package com.aplazo.bnpl.dto.response;

import com.aplazo.bnpl.exception.ErrorCode;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String error,
        Long timestamp,
        String message,
        String path
) {
    public static ErrorResponse of(String code, String error, String message, String path) {
        return new ErrorResponse(code, error, Instant.now().getEpochSecond(), message, path);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getError(), Instant.now().getEpochSecond(), message, path);
    }
}
