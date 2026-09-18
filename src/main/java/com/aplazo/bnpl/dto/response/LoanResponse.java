package com.aplazo.bnpl.dto.response;

import com.aplazo.bnpl.entity.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID customerId,
        BigDecimal amount,
        LoanStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,
        PaymentPlanResponse paymentPlan
) {
}
