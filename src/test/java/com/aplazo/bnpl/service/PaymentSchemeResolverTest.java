package com.aplazo.bnpl.service;

import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.PaymentScheme;
import com.aplazo.bnpl.service.model.InstallmentCalculation;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSchemeResolverTest {

    private PaymentSchemeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PaymentSchemeResolver();
    }

    @Nested
    @DisplayName("Scheme Resolution Rules")
    class SchemeResolutionTests {

        @ParameterizedTest
        @ValueSource(strings = {"Carlos", "carlos", "CARLOS", "Luna", "luna", "Hugo", "hugo", "C", "l", "H"})
        @DisplayName("Rule 1: Names starting with 'C', 'L', or 'H' (case-insensitive) resolve to SCHEME_1")
        void shouldResolveScheme1WhenNameStartsWithCLH(String firstName) {
            // Even if internalCustomerId <= 25 or > 25, Rule 1 takes precedence
            assertThat(resolver.resolveScheme(firstName, 10L)).isEqualTo(PaymentScheme.SCHEME_1);
            assertThat(resolver.resolveScheme(firstName, 30L)).isEqualTo(PaymentScheme.SCHEME_1);
        }

        @Test
        @DisplayName("Rule 2: Name does not start with C/L/H, but customerId > 25 resolves to SCHEME_2")
        void shouldResolveScheme2WhenCustomerIdGreaterThan25() {
            assertThat(resolver.resolveScheme("Juan", 26L)).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(resolver.resolveScheme("Pedro", 100L)).isEqualTo(PaymentScheme.SCHEME_2);
        }

        @Test
        @DisplayName("Rule 3: Name does not start with C/L/H and customerId <= 25 falls back to SCHEME_2")
        void shouldResolveScheme2WhenCustomerIdLessThanOrEqualTo25() {
            assertThat(resolver.resolveScheme("Juan", 25L)).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(resolver.resolveScheme("Juan", 1L)).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(resolver.resolveScheme("Juan", 0L)).isEqualTo(PaymentScheme.SCHEME_2);
        }

        @Test
        @DisplayName("Fallback to SCHEME_2 when firstName is null or empty")
        void shouldHandleNullOrEmptyFirstName() {
            assertThat(resolver.resolveScheme(null, 10L)).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(resolver.resolveScheme("", 10L)).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(resolver.resolveScheme("   ", 10L)).isEqualTo(PaymentScheme.SCHEME_2);
        }
    }

    @Nested
    @DisplayName("Payment Plan Calculation")
    class PlanCalculationTests {

        private final LocalDate purchaseDate = LocalDate.of(2026, 9, 18);

        @Test
        @DisplayName("Should calculate plan correctly for SCHEME_1 (13% commission) with exact division")
        void shouldCalculatePlanForScheme1Exact() {
            BigDecimal amount = new BigDecimal("1000.00");
            // Commission: 1000.00 * 0.13 = 130.00
            // Total: 1130.00
            // 1130.00 / 5 = 226.00 exactly (remainder 0)
            PaymentPlanCalculation plan = resolver.calculatePlan(amount, PaymentScheme.SCHEME_1, purchaseDate);

            assertThat(plan.scheme()).isEqualTo(PaymentScheme.SCHEME_1);
            assertThat(plan.commissionAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
            assertThat(plan.totalAmount()).isEqualByComparingTo(new BigDecimal("1130.00"));
            assertThat(plan.installments()).hasSize(5);

            // Verify installment schedule dates and statuses
            InstallmentCalculation inst1 = plan.installments().get(0);
            assertThat(inst1.installmentNumber()).isEqualTo(1);
            assertThat(inst1.scheduledPaymentDate()).isEqualTo(purchaseDate.plusDays(14));
            assertThat(inst1.status()).isEqualTo(InstallmentStatus.NEXT);
            assertThat(inst1.amount()).isEqualByComparingTo(new BigDecimal("226.00"));

            for (int i = 1; i < 5; i++) {
                InstallmentCalculation inst = plan.installments().get(i);
                assertThat(inst.installmentNumber()).isEqualTo(i + 1);
                assertThat(inst.scheduledPaymentDate()).isEqualTo(purchaseDate.plusDays(14L * (i + 1)));
                assertThat(inst.status()).isEqualTo(InstallmentStatus.PENDING);
                assertThat(inst.amount()).isEqualByComparingTo(new BigDecimal("226.00"));
            }

            // Verify sum of installments equals totalAmount
            BigDecimal sum = plan.installments().stream()
                    .map(InstallmentCalculation::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(plan.totalAmount());
        }

        @Test
        @DisplayName("Should calculate plan correctly for SCHEME_2 (16% commission) with remainder penny on 5th installment")
        void shouldCalculatePlanForScheme2WithRemainder() {
            BigDecimal amount = new BigDecimal("150.00");
            // Commission: 150.00 * 0.16 = 24.00
            // Total: 174.00
            // 174.00 / 5 = 34.80 exactly
            // Let's test amount = 100.00 with SCHEME_2:
            // 100.00 * 0.16 = 16.00
            // Total: 116.00
            // 116.00 / 5 = base 23.20
            // 23.20 * 5 = 116.00 (exact)
            // Let's use an amount that produces fractional pennies, e.g., 100.01:
            // Commission: 100.01 * 0.16 = 16.0016 -> HALF_EVEN = 16.00
            // Total = 116.01
            // 116.01 / 5 = 23.20 (FLOOR)
            // base * 5 = 116.00
            // remainder = 0.01
            // Inst 1..4: 23.20, Inst 5: 23.21
            BigDecimal oddAmount = new BigDecimal("100.01");
            PaymentPlanCalculation plan = resolver.calculatePlan(oddAmount, PaymentScheme.SCHEME_2, purchaseDate);

            assertThat(plan.scheme()).isEqualTo(PaymentScheme.SCHEME_2);
            assertThat(plan.commissionAmount()).isEqualByComparingTo(new BigDecimal("16.00"));
            assertThat(plan.totalAmount()).isEqualByComparingTo(new BigDecimal("116.01"));
            assertThat(plan.installments()).hasSize(5);

            assertThat(plan.installments().get(0).amount()).isEqualByComparingTo(new BigDecimal("23.20"));
            assertThat(plan.installments().get(1).amount()).isEqualByComparingTo(new BigDecimal("23.20"));
            assertThat(plan.installments().get(2).amount()).isEqualByComparingTo(new BigDecimal("23.20"));
            assertThat(plan.installments().get(3).amount()).isEqualByComparingTo(new BigDecimal("23.20"));
            assertThat(plan.installments().get(4).amount()).isEqualByComparingTo(new BigDecimal("23.21"));

            BigDecimal sum = plan.installments().stream()
                    .map(InstallmentCalculation::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(plan.totalAmount());
        }

        @Test
        @DisplayName("Should test odd cent rounding case (e.g. amount = 1000.03)")
        void shouldHandleMultiCentResidualRounding() {
            BigDecimal amount = new BigDecimal("1000.03");
            // Commission: 1000.03 * 0.13 = 130.0039 -> 130.00
            // Total: 1130.03
            // Base: 1130.03 / 5 = 226.00
            // Base * 5 = 1130.00
            // Remainder: 0.03
            // Inst 1..4: 226.00, Inst 5: 226.03
            PaymentPlanCalculation plan = resolver.calculatePlan(amount, PaymentScheme.SCHEME_1, purchaseDate);

            assertThat(plan.commissionAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
            assertThat(plan.totalAmount()).isEqualByComparingTo(new BigDecimal("1130.03"));
            for (int i = 0; i < 4; i++) {
                assertThat(plan.installments().get(i).amount()).isEqualByComparingTo(new BigDecimal("226.00"));
            }
            assertThat(plan.installments().get(4).amount()).isEqualByComparingTo(new BigDecimal("226.03"));

            BigDecimal sum = plan.installments().stream()
                    .map(InstallmentCalculation::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(plan.totalAmount());
        }
    }
}
