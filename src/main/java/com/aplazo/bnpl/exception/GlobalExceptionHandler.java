package com.aplazo.bnpl.exception;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCustomerRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCustomerRequestException(
            InvalidCustomerRequestException ex, HttpServletRequest request) {
        log.warn("Invalid customer request at {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_CUSTOMER_REQUEST,
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidLoanRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLoanRequestException(
            InvalidLoanRequestException ex, HttpServletRequest request) {
        log.warn("Invalid loan request at {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_LOAN_REQUEST,
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(
            CustomerNotFoundException ex, HttpServletRequest request) {
        log.warn("Customer not found at {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.CUSTOMER_NOT_FOUND,
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoanNotFoundException(
            LoanNotFoundException ex, HttpServletRequest request) {
        log.warn("Loan not found at {}: {}", getPath(request), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.LOAN_NOT_FOUND,
                ex.getMessage(),
                getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String path = getPath(request);
        ErrorCode errorCode = resolveValidationErrorCode(ex.getBindingResult().getTarget(), path);

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ex.getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }
        if (message.isBlank()) {
            message = "Validation failed for request";
        }

        log.warn("Validation failure at {} [{}]: {}", path, errorCode.getCode(), message);
        ErrorResponse response = ErrorResponse.of(errorCode, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        String path = getPath(request);
        ErrorCode errorCode = path.contains("/customers")
                ? ErrorCode.INVALID_CUSTOMER_REQUEST
                : path.contains("/loans")
                ? ErrorCode.INVALID_LOAN_REQUEST
                : ErrorCode.INVALID_REQUEST;

        String message = ex.getAllErrors().stream()
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ex.getMessage();
        }

        log.warn("Handler method validation failure at {}: {}", path, message);
        ErrorResponse response = ErrorResponse.of(errorCode, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String path = getPath(request);
        ErrorCode errorCode = path.contains("/customers")
                ? ErrorCode.INVALID_CUSTOMER_REQUEST
                : path.contains("/loans")
                ? ErrorCode.INVALID_LOAN_REQUEST
                : ErrorCode.INVALID_REQUEST;

        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ex.getMessage();
        }

        log.warn("Constraint violation at {}: {}", path, message);
        ErrorResponse response = ErrorResponse.of(errorCode, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String path = getPath(request);
        ErrorCode errorCode = path.contains("/customers")
                ? ErrorCode.INVALID_CUSTOMER_REQUEST
                : path.contains("/loans")
                ? ErrorCode.INVALID_LOAN_REQUEST
                : ErrorCode.INVALID_REQUEST;

        String message = "Malformed or unreadable request payload";
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isBlank()) {
            message = rootCause.getMessage();
        }

        log.warn("Unreadable HTTP message at {}: {}", path, message);
        ErrorResponse response = ErrorResponse.of(errorCode, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String path = getPath(request);
        String message = String.format("Failed to convert parameter '%s' with value '%s' to required type",
                ex.getName(), ex.getValue());
        log.warn("Argument type mismatch at {}: {}", path, message);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        String path = getPath(request);
        log.warn("Resource not found at {}: {}", path, ex.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String path = getPath(request);
        log.error("Unhandled exception processing request at {}", path, ex);
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Internal server error";
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, message, path);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorCode resolveValidationErrorCode(Object target, String path) {
        if (target instanceof CustomerRequest || path.contains("/customers")) {
            return ErrorCode.INVALID_CUSTOMER_REQUEST;
        }
        if (target instanceof LoanRequest || path.contains("/loans")) {
            return ErrorCode.INVALID_LOAN_REQUEST;
        }
        return ErrorCode.INVALID_REQUEST;
    }

    private String getPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }
}
