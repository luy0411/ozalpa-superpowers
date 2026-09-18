package com.aplazo.bnpl.service;

import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.PaymentScheme;
import com.aplazo.bnpl.service.model.InstallmentCalculation;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentSchemeResolver {

    private static final int TOTAL_INSTALLMENTS = 5;
    private static final long DAYS_BETWEEN_INSTALLMENTS = 14L;

    public PaymentScheme resolveScheme(String firstName, Long internalCustomerId) {
        if (firstName != null && !firstName.isBlank()) {
            char firstChar = Character.toUpperCase(firstName.trim().charAt(0));
            if (firstChar == 'C' || firstChar == 'L' || firstChar == 'H') {
                return PaymentScheme.SCHEME_1;
            }
        }

        if (internalCustomerId != null && internalCustomerId > 25) {
            return PaymentScheme.SCHEME_2;
        }

        return PaymentScheme.SCHEME_2;
    }

    public PaymentPlanCalculation calculatePlan(BigDecimal amount, PaymentScheme scheme, LocalDate purchaseDate) {
        if (amount == null || scheme == null || purchaseDate == null) {
            throw new IllegalArgumentException("Amount, scheme, and purchaseDate cannot be null");
        }

        BigDecimal commissionAmount = amount.multiply(scheme.getInterestRate())
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalAmount = amount.add(commissionAmount);

        BigDecimal baseInstallmentAmount = totalAmount.divide(
                BigDecimal.valueOf(TOTAL_INSTALLMENTS),
                2,
                RoundingMode.FLOOR
        );

        BigDecimal remainder = totalAmount.subtract(baseInstallmentAmount.multiply(BigDecimal.valueOf(TOTAL_INSTALLMENTS)));

        List<InstallmentCalculation> installments = new ArrayList<>(TOTAL_INSTALLMENTS);
        for (int i = 1; i <= TOTAL_INSTALLMENTS; i++) {
            LocalDate scheduledDate = purchaseDate.plusDays(DAYS_BETWEEN_INSTALLMENTS * i);
            InstallmentStatus status = (i == 1) ? InstallmentStatus.NEXT : InstallmentStatus.PENDING;
            BigDecimal installmentAmount = (i == TOTAL_INSTALLMENTS)
                    ? baseInstallmentAmount.add(remainder)
                    : baseInstallmentAmount;

            installments.add(new InstallmentCalculation(i, installmentAmount, scheduledDate, status));
        }

        return new PaymentPlanCalculation(commissionAmount, totalAmount, scheme, List.copyOf(installments));
    }
}
