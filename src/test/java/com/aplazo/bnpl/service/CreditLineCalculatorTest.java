package com.aplazo.bnpl.service;

import com.aplazo.bnpl.exception.InvalidCustomerRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditLineCalculatorTest {

    private CreditLineCalculator calculator;
    private final LocalDate referenceDate = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        calculator = new CreditLineCalculator();
    }

    @Test
    @DisplayName("Should assign 3000.00 for customer aged 18")
    void shouldAssign3000ForAge18() {
        LocalDate birthDate = referenceDate.minusYears(18);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Should assign 3000.00 for customer aged 20")
    void shouldAssign3000ForAge20() {
        LocalDate birthDate = referenceDate.minusYears(20);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Should assign 3000.00 for customer aged 25")
    void shouldAssign3000ForAge25() {
        LocalDate birthDate = referenceDate.minusYears(25);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Should assign 5000.00 for customer aged 26")
    void shouldAssign5000ForAge26() {
        LocalDate birthDate = referenceDate.minusYears(26);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Should assign 5000.00 for customer aged 28")
    void shouldAssign5000ForAge28() {
        LocalDate birthDate = referenceDate.minusYears(28);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Should assign 5000.00 for customer aged 30")
    void shouldAssign5000ForAge30() {
        LocalDate birthDate = referenceDate.minusYears(30);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Should assign 8000.00 for customer aged 31")
    void shouldAssign8000ForAge31() {
        LocalDate birthDate = referenceDate.minusYears(31);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("8000.00"));
    }

    @Test
    @DisplayName("Should assign 8000.00 for customer aged 45")
    void shouldAssign8000ForAge45() {
        LocalDate birthDate = referenceDate.minusYears(45);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("8000.00"));
    }

    @Test
    @DisplayName("Should assign 8000.00 for customer aged 65")
    void shouldAssign8000ForAge65() {
        LocalDate birthDate = referenceDate.minusYears(65);
        BigDecimal creditLine = calculator.calculateCreditLine(birthDate, referenceDate);
        assertThat(creditLine).isEqualByComparingTo(new BigDecimal("8000.00"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 16, 17})
    @DisplayName("Should throw InvalidCustomerRequestException for customers under 18")
    void shouldThrowForUnder18(int age) {
        LocalDate birthDate = referenceDate.minusYears(age);
        assertThatThrownBy(() -> calculator.calculateCreditLine(birthDate, referenceDate))
                .isInstanceOf(InvalidCustomerRequestException.class)
                .hasMessageContaining("Customer must be between 18 and 65 years old");
    }

    @ParameterizedTest
    @ValueSource(ints = {66, 70, 90})
    @DisplayName("Should throw InvalidCustomerRequestException for customers over 65")
    void shouldThrowForOver65(int age) {
        LocalDate birthDate = referenceDate.minusYears(age);
        assertThatThrownBy(() -> calculator.calculateCreditLine(birthDate, referenceDate))
                .isInstanceOf(InvalidCustomerRequestException.class)
                .hasMessageContaining("Customer must be between 18 and 65 years old");
    }

    @Test
    @DisplayName("Should throw InvalidCustomerRequestException when birthDate is null or in the future")
    void shouldThrowWhenBirthDateInvalid() {
        assertThatThrownBy(() -> calculator.calculateCreditLine(null, referenceDate))
                .isInstanceOf(InvalidCustomerRequestException.class);

        LocalDate future = referenceDate.plusDays(1);
        assertThatThrownBy(() -> calculator.calculateCreditLine(future, referenceDate))
                .isInstanceOf(InvalidCustomerRequestException.class);
    }
}
