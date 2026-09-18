package com.aplazo.bnpl.mapper;

import com.aplazo.bnpl.dto.response.InstallmentResponse;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.dto.response.PaymentPlanResponse;
import com.aplazo.bnpl.entity.InstallmentEntity;
import com.aplazo.bnpl.entity.LoanEntity;
import com.aplazo.bnpl.service.model.InstallmentCalculation;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;

import java.util.Collections;
import java.util.List;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static InstallmentResponse toInstallmentResponse(InstallmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InstallmentResponse(
                entity.getAmount(),
                entity.getScheduledPaymentDate(),
                entity.getStatus()
        );
    }

    public static InstallmentResponse toInstallmentResponse(InstallmentCalculation calculation) {
        if (calculation == null) {
            return null;
        }
        return new InstallmentResponse(
                calculation.amount(),
                calculation.scheduledPaymentDate(),
                calculation.status()
        );
    }

    public static PaymentPlanResponse toPaymentPlanResponse(PaymentPlanCalculation calculation) {
        if (calculation == null) {
            return null;
        }
        List<InstallmentResponse> installments = calculation.installments() != null
                ? calculation.installments().stream().map(LoanMapper::toInstallmentResponse).toList()
                : Collections.emptyList();
        return new PaymentPlanResponse(calculation.commissionAmount(), installments);
    }

    public static PaymentPlanResponse toPaymentPlanResponse(LoanEntity loan) {
        if (loan == null) {
            return null;
        }
        List<InstallmentResponse> installments = loan.getInstallments() != null
                ? loan.getInstallments().stream().map(LoanMapper::toInstallmentResponse).toList()
                : Collections.emptyList();
        return new PaymentPlanResponse(loan.getCommissionAmount(), installments);
    }

    public static LoanResponse toResponse(LoanEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LoanResponse(
                entity.getExternalId(),
                entity.getCustomer() != null ? entity.getCustomer().getExternalId() : null,
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                toPaymentPlanResponse(entity)
        );
    }

    public static LoanResponse toResponse(LoanEntity entity, PaymentPlanResponse paymentPlan) {
        if (entity == null) {
            return null;
        }
        return new LoanResponse(
                entity.getExternalId(),
                entity.getCustomer() != null ? entity.getCustomer().getExternalId() : null,
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                paymentPlan
        );
    }

    public static LoanResponse toResponse(LoanEntity entity, PaymentPlanCalculation calculation) {
        if (entity == null) {
            return null;
        }
        return new LoanResponse(
                entity.getExternalId(),
                entity.getCustomer() != null ? entity.getCustomer().getExternalId() : null,
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                toPaymentPlanResponse(calculation)
        );
    }
}
