package com.aplazo.bnpl.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PaymentPlanResponse(
        BigDecimal commissionAmount,
        List<InstallmentResponse> installments
) {
}
