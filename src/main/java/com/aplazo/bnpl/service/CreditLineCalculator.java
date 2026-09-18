package com.aplazo.bnpl.service;

import com.aplazo.bnpl.exception.InvalidCustomerRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
public class CreditLineCalculator {

    private static final BigDecimal LIMIT_BRACKET_18_TO_25 = new BigDecimal("3000.00");
    private static final BigDecimal LIMIT_BRACKET_26_TO_30 = new BigDecimal("5000.00");
    private static final BigDecimal LIMIT_BRACKET_31_TO_65 = new BigDecimal("8000.00");

    public BigDecimal calculateCreditLine(LocalDate birthDate, LocalDate referenceDate) {
        if (birthDate == null || referenceDate == null) {
            throw new InvalidCustomerRequestException("Customer birth date and reference date cannot be null");
        }
        if (birthDate.isAfter(referenceDate)) {
            throw new InvalidCustomerRequestException("Customer birth date cannot be in the future");
        }

        int age = Period.between(birthDate, referenceDate).getYears();
        if (age < 18 || age > 65) {
            throw new InvalidCustomerRequestException("Customer must be between 18 and 65 years old");
        }

        if (age <= 25) {
            return LIMIT_BRACKET_18_TO_25;
        } else if (age <= 30) {
            return LIMIT_BRACKET_26_TO_30;
        } else {
            return LIMIT_BRACKET_31_TO_65;
        }
    }
}
