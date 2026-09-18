package com.aplazo.bnpl.exception;

public enum ErrorCode {
    INTERNAL_SERVER_ERROR("APZ000001", "INTERNAL_SERVER_ERROR"),
    INVALID_CUSTOMER_REQUEST("APZ000002", "INVALID_CUSTOMER_REQUEST"),
    RATE_LIMIT_ERROR("APZ000003", "RATE_LIMIT_ERROR"),
    INVALID_REQUEST("APZ000004", "INVALID_REQUEST"),
    CUSTOMER_NOT_FOUND("APZ000005", "CUSTOMER_NOT_FOUND"),
    INVALID_LOAN_REQUEST("APZ000006", "INVALID_LOAN_REQUEST"),
    UNAUTHORIZED("APZ000007", "UNAUTHORIZED"),
    LOAN_NOT_FOUND("APZ000008", "LOAN_NOT_FOUND");

    private final String code;
    private final String error;

    ErrorCode(String code, String error) {
        this.code = code;
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public String getError() {
        return error;
    }
}
