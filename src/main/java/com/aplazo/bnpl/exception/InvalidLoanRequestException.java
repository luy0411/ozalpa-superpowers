package com.aplazo.bnpl.exception;

public class InvalidLoanRequestException extends RuntimeException {

    public InvalidLoanRequestException(String message) {
        super(message);
    }

    public InvalidLoanRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
