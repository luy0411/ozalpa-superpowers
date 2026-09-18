package com.aplazo.bnpl.dto.response;

import com.aplazo.bnpl.entity.InstallmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        BigDecimal amount,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scheduledPaymentDate,
        InstallmentStatus status
) {
}
