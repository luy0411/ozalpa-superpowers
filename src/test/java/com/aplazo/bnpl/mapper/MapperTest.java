package com.aplazo.bnpl.mapper;

import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.dto.response.InstallmentResponse;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.dto.response.PaymentPlanResponse;
import com.aplazo.bnpl.entity.CustomerEntity;
import com.aplazo.bnpl.entity.InstallmentEntity;
import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.LoanEntity;
import com.aplazo.bnpl.entity.LoanStatus;
import com.aplazo.bnpl.entity.PaymentScheme;
import com.aplazo.bnpl.service.model.InstallmentCalculation;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mapper Tests")
class MapperTest {

    @Test
    @DisplayName("CustomerMapper converts CustomerEntity to CustomerResponse")
    void testCustomerMapper() {
        UUID externalId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        CustomerEntity customer = CustomerEntity.builder()
                .externalId(externalId)
                .firstName("Pepe")
                .lastName("García")
                .secondLastName("Flores")
                .dateOfBirth(LocalDate.of(1998, 7, 21))
                .creditLineAmount(BigDecimal.valueOf(1000.00))
                .availableCreditLineAmount(BigDecimal.valueOf(800.00))
                .createdAt(createdAt)
                .build();

        CustomerResponse response = CustomerMapper.toResponse(customer);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(externalId);
        assertThat(response.creditLineAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
        assertThat(response.availableCreditLineAmount()).isEqualByComparingTo(BigDecimal.valueOf(800.00));
        assertThat(response.createdAt()).isEqualTo(createdAt);

        assertThat(CustomerMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("LoanMapper converts LoanEntity and Installments to LoanResponse")
    void testLoanMapperFromEntities() {
        UUID customerId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        CustomerEntity customer = CustomerEntity.builder()
                .externalId(customerId)
                .build();

        InstallmentEntity installment1 = InstallmentEntity.builder()
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .scheduledPaymentDate(LocalDate.of(2026, 9, 25))
                .status(InstallmentStatus.NEXT)
                .build();

        InstallmentEntity installment2 = InstallmentEntity.builder()
                .installmentNumber(2)
                .amount(BigDecimal.valueOf(100.00))
                .scheduledPaymentDate(LocalDate.of(2026, 10, 2))
                .status(InstallmentStatus.PENDING)
                .build();

        LoanEntity loan = LoanEntity.builder()
                .externalId(loanId)
                .customer(customer)
                .amount(BigDecimal.valueOf(500.00))
                .commissionAmount(BigDecimal.valueOf(50.00))
                .totalAmount(BigDecimal.valueOf(550.00))
                .schemeName(PaymentScheme.SCHEME_1)
                .interestRate(BigDecimal.valueOf(0.13))
                .status(LoanStatus.ACTIVE)
                .createdAt(createdAt)
                .installments(List.of(installment1, installment2))
                .build();

        LoanResponse response = LoanMapper.toResponse(loan);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(loanId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(createdAt);

        PaymentPlanResponse plan = response.paymentPlan();
        assertThat(plan).isNotNull();
        assertThat(plan.commissionAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(plan.installments()).hasSize(2);
        assertThat(plan.installments().get(0).status()).isEqualTo(InstallmentStatus.NEXT);
        assertThat(plan.installments().get(1).status()).isEqualTo(InstallmentStatus.PENDING);

        assertThat(LoanMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("LoanMapper converts PaymentPlanCalculation to responses")
    void testLoanMapperFromCalculation() {
        PaymentPlanCalculation calculation = new PaymentPlanCalculation(
                BigDecimal.valueOf(45.00),
                BigDecimal.valueOf(495.00),
                PaymentScheme.SCHEME_2,
                List.of(
                        new InstallmentCalculation(1, BigDecimal.valueOf(99.00), LocalDate.of(2026, 9, 25), InstallmentStatus.NEXT),
                        new InstallmentCalculation(2, BigDecimal.valueOf(99.00), LocalDate.of(2026, 10, 9), InstallmentStatus.PENDING)
                )
        );

        PaymentPlanResponse planResponse = LoanMapper.toPaymentPlanResponse(calculation);
        assertThat(planResponse).isNotNull();
        assertThat(planResponse.commissionAmount()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        assertThat(planResponse.installments()).hasSize(2);

        InstallmentResponse instResponse = LoanMapper.toInstallmentResponse(calculation.installments().get(0));
        assertThat(instResponse.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.00));
        assertThat(instResponse.status()).isEqualTo(InstallmentStatus.NEXT);
        assertThat(instResponse.scheduledPaymentDate()).isEqualTo(LocalDate.of(2026, 9, 25));

        UUID loanId = UUID.randomUUID();
        CustomerEntity customer = CustomerEntity.builder().externalId(UUID.randomUUID()).build();
        LoanEntity loan = LoanEntity.builder()
                .externalId(loanId)
                .customer(customer)
                .amount(BigDecimal.valueOf(450.00))
                .status(LoanStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        LoanResponse loanResponse = LoanMapper.toResponse(loan, calculation);
        assertThat(loanResponse).isNotNull();
        assertThat(loanResponse.id()).isEqualTo(loanId);
        assertThat(loanResponse.paymentPlan().commissionAmount()).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        assertThat(loanResponse.paymentPlan().installments()).hasSize(2);
    }
}
