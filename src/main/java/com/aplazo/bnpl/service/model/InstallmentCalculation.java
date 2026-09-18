package com.aplazo.bnpl.service.model;

import com.aplazo.bnpl.entity.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentCalculation(
        int installmentNumber,
        BigDecimal amount,
        LocalDate scheduledPaymentDate,
        InstallmentStatus status
) {
}
