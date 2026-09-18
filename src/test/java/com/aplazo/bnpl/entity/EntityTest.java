package com.aplazo.bnpl.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTest {

    @Test
    void testPaymentScheme() {
        assertThat(PaymentScheme.SCHEME_1.getRate()).isEqualByComparingTo("0.13");
        assertThat(PaymentScheme.SCHEME_1.getInterestRate()).isEqualByComparingTo("0.13");
        assertThat(PaymentScheme.SCHEME_2.getRate()).isEqualByComparingTo("0.16");
        assertThat(PaymentScheme.SCHEME_2.getInterestRate()).isEqualByComparingTo("0.16");
        assertThat(PaymentScheme.valueOf("SCHEME_1")).isEqualTo(PaymentScheme.SCHEME_1);
    }

    @Test
    void testLoanStatusAndInstallmentStatus() {
        assertThat(LoanStatus.values()).containsExactly(LoanStatus.ACTIVE, LoanStatus.LATE, LoanStatus.COMPLETED);
        assertThat(InstallmentStatus.values()).containsExactly(InstallmentStatus.NEXT, InstallmentStatus.PENDING, InstallmentStatus.ERROR);
    }

    @Test
    void testCustomerEntity() {
        UUID externalId = UUID.randomUUID();
        LocalDate dob = LocalDate.of(1990, 1, 1);
        Instant now = Instant.now();

        CustomerEntity customer = CustomerEntity.builder()
                .id(1L)
                .externalId(externalId)
                .firstName("John")
                .lastName("Doe")
                .secondLastName("Smith")
                .dateOfBirth(dob)
                .creditLineAmount(new BigDecimal("5000.00"))
                .availableCreditLineAmount(new BigDecimal("5000.00"))
                .createdAt(now)
                .build();

        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(customer.getExternalId()).isEqualTo(externalId);
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getSecondLastName()).isEqualTo("Smith");
        assertThat(customer.getDateOfBirth()).isEqualTo(dob);
        assertThat(customer.getCreditLineAmount()).isEqualByComparingTo("5000.00");
        assertThat(customer.getAvailableCreditLineAmount()).isEqualByComparingTo("5000.00");
        assertThat(customer.getCreatedAt()).isEqualTo(now);
        assertThat(customer.toString()).contains("John", "Doe");

        CustomerEntity customer2 = new CustomerEntity();
        customer2.setId(2L);
        customer2.setExternalId(externalId);
        customer2.setFirstName("Jane");
        customer2.setLastName("Doe");
        customer2.setSecondLastName("Smith");
        customer2.setDateOfBirth(dob);
        customer2.setCreditLineAmount(new BigDecimal("3000.00"));
        customer2.setAvailableCreditLineAmount(new BigDecimal("3000.00"));
        customer2.setCreatedAt(now);

        assertThat(customer).isEqualTo(customer2);
        assertThat(customer.hashCode()).isEqualTo(customer2.hashCode());

        CustomerEntity newCustomer = new CustomerEntity();
        newCustomer.onCreate();
        assertThat(newCustomer.getCreatedAt()).isNotNull();
        assertThat(newCustomer.getExternalId()).isNotNull();
    }

    @Test
    void testLoanAndInstallmentEntities() {
        UUID loanExternalId = UUID.randomUUID();
        CustomerEntity customer = CustomerEntity.builder().id(1L).externalId(UUID.randomUUID()).build();
        Instant now = Instant.now();

        LoanEntity loan = LoanEntity.builder()
                .id(10L)
                .externalId(loanExternalId)
                .customer(customer)
                .amount(new BigDecimal("1000.00"))
                .commissionAmount(new BigDecimal("130.00"))
                .totalAmount(new BigDecimal("1130.00"))
                .schemeName(PaymentScheme.SCHEME_1)
                .interestRate(new BigDecimal("0.1300"))
                .status(LoanStatus.ACTIVE)
                .createdAt(now)
                .build();

        InstallmentEntity installment = InstallmentEntity.builder()
                .id(100L)
                .installmentNumber(1)
                .amount(new BigDecimal("226.00"))
                .scheduledPaymentDate(LocalDate.now().plusDays(14))
                .status(InstallmentStatus.NEXT)
                .createdAt(now)
                .build();

        loan.addInstallment(installment);

        assertThat(loan.getInstallments()).hasSize(1);
        assertThat(installment.getLoan()).isEqualTo(loan);
        assertThat(loan.getId()).isEqualTo(10L);
        assertThat(loan.getExternalId()).isEqualTo(loanExternalId);
        assertThat(loan.getCustomer()).isEqualTo(customer);
        assertThat(loan.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(loan.getCommissionAmount()).isEqualByComparingTo("130.00");
        assertThat(loan.getTotalAmount()).isEqualByComparingTo("1130.00");
        assertThat(loan.getSchemeName()).isEqualTo(PaymentScheme.SCHEME_1);
        assertThat(loan.getInterestRate()).isEqualByComparingTo("0.1300");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getCreatedAt()).isEqualTo(now);
        assertThat(loan.toString()).contains("1130.00");

        LoanEntity loan2 = new LoanEntity();
        loan2.setId(20L);
        loan2.setExternalId(loanExternalId);
        assertThat(loan).isEqualTo(loan2);
        assertThat(loan.hashCode()).isEqualTo(loan2.hashCode());

        LoanEntity newLoan = new LoanEntity();
        newLoan.onCreate();
        assertThat(newLoan.getCreatedAt()).isNotNull();
        assertThat(newLoan.getExternalId()).isNotNull();

        loan.removeInstallment(installment);
        assertThat(loan.getInstallments()).isEmpty();
        assertThat(installment.getLoan()).isNull();

        InstallmentEntity inst2 = new InstallmentEntity();
        inst2.setId(100L);
        inst2.setLoan(loan);
        inst2.setInstallmentNumber(1);
        inst2.setAmount(new BigDecimal("226.00"));
        inst2.setScheduledPaymentDate(LocalDate.now().plusDays(14));
        inst2.setStatus(InstallmentStatus.NEXT);
        inst2.setCreatedAt(now);

        installment.setId(100L);
        assertThat(installment).isEqualTo(inst2);
        assertThat(installment.hashCode()).isEqualTo(inst2.hashCode());
        assertThat(inst2.toString()).contains("226.00");

        InstallmentEntity newInst = new InstallmentEntity();
        newInst.onCreate();
        assertThat(newInst.getCreatedAt()).isNotNull();
        assertThat(newInst).isEqualTo(newInst);
        assertThat(newInst).isNotEqualTo(new InstallmentEntity());

        InstallmentEntity inst3 = new InstallmentEntity();
        inst3.setId(100L);
        assertThat(installment).isEqualTo(inst2);
        assertThat(inst2).isEqualTo(inst3);
        assertThat(installment).isEqualTo(inst3);

        LoanEntity loanWithList = new LoanEntity(1L, UUID.randomUUID(), customer, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.valueOf(11), PaymentScheme.SCHEME_2, new BigDecimal("0.1600"), LoanStatus.ACTIVE, now,
                Collections.singletonList(inst2));
        assertThat(loanWithList.getInstallments()).hasSize(1);
        assertThat(inst2.getLoan()).isEqualTo(loanWithList);

        CustomerEntity customerProxySubclass = new CustomerEntity() {
            @Override
            public UUID getExternalId() {
                return customer.getExternalId();
            }
        };
        assertThat(customer).isEqualTo(customerProxySubclass);

        LoanEntity loanProxySubclass = new LoanEntity() {
            @Override
            public UUID getExternalId() {
                return loanExternalId;
            }
        };
        assertThat(loan).isEqualTo(loanProxySubclass);
    }
}
