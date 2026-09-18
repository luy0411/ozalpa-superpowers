package com.aplazo.bnpl.service.model;

import com.aplazo.bnpl.entity.PaymentScheme;

import java.math.BigDecimal;
import java.util.List;

public record PaymentPlanCalculation(
        BigDecimal commissionAmount,
        BigDecimal totalAmount,
        PaymentScheme scheme,
        List<InstallmentCalculation> installments
) {
}
