package com.aplazo.bnpl.service;

import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.entity.CustomerEntity;
import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.LoanEntity;
import com.aplazo.bnpl.entity.LoanStatus;
import com.aplazo.bnpl.entity.PaymentScheme;
import com.aplazo.bnpl.exception.CustomerNotFoundException;
import com.aplazo.bnpl.exception.InvalidLoanRequestException;
import com.aplazo.bnpl.exception.LoanNotFoundException;
import com.aplazo.bnpl.repository.CustomerRepository;
import com.aplazo.bnpl.repository.LoanRepository;
import com.aplazo.bnpl.service.model.InstallmentCalculation;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentSchemeResolver paymentSchemeResolver;

    private Clock fixedClock;
    private LoanService loanService;

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        loanService = new LoanService(loanRepository, customerRepository, paymentSchemeResolver, fixedClock);
    }

    @Nested
    @DisplayName("createLoan Tests")
    class CreateLoanTests {

        @Test
        @DisplayName("Should successfully create loan, deduct credit atomically, resolve scheme and save installments")
        void shouldCreateLoanSuccessfully() {
            UUID customerId = UUID.randomUUID();
            BigDecimal initialCredit = new BigDecimal("5000.00");
            BigDecimal requestAmount = new BigDecimal("1000.00");

            CustomerEntity customer = CustomerEntity.builder()
                    .id(10L)
                    .externalId(customerId)
                    .firstName("Carlos")
                    .lastName("Lopez")
                    .availableCreditLineAmount(initialCredit)
                    .creditLineAmount(initialCredit)
                    .build();

            LoanRequest request = new LoanRequest(customerId, requestAmount);

            PaymentScheme resolvedScheme = PaymentScheme.SCHEME_1;
            List<InstallmentCalculation> installments = List.of(
                    new InstallmentCalculation(1, new BigDecimal("224.00"), FIXED_DATE.plusDays(14), InstallmentStatus.NEXT),
                    new InstallmentCalculation(2, new BigDecimal("224.00"), FIXED_DATE.plusDays(28), InstallmentStatus.PENDING),
                    new InstallmentCalculation(3, new BigDecimal("224.00"), FIXED_DATE.plusDays(42), InstallmentStatus.PENDING),
                    new InstallmentCalculation(4, new BigDecimal("224.00"), FIXED_DATE.plusDays(56), InstallmentStatus.PENDING),
                    new InstallmentCalculation(5, new BigDecimal("224.00"), FIXED_DATE.plusDays(70), InstallmentStatus.PENDING)
            );
            PaymentPlanCalculation planCalculation = new PaymentPlanCalculation(
                    new BigDecimal("120.00"),
                    new BigDecimal("1120.00"),
                    resolvedScheme,
                    installments
            );

            when(customerRepository.findByExternalId(customerId)).thenReturn(Optional.of(customer));
            when(paymentSchemeResolver.resolveScheme("Carlos", 10L)).thenReturn(resolvedScheme);
            when(paymentSchemeResolver.calculatePlan(requestAmount, resolvedScheme, FIXED_DATE)).thenReturn(planCalculation);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanResponse response = loanService.createLoan(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.customerId()).isEqualTo(customerId);
            assertThat(response.amount()).isEqualByComparingTo(requestAmount);
            assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(response.createdAt()).isNotNull();

            assertThat(response.paymentPlan()).isNotNull();
            assertThat(response.paymentPlan().commissionAmount()).isEqualByComparingTo("120.00");
            assertThat(response.paymentPlan().installments()).hasSize(5);
            assertThat(response.paymentPlan().installments().get(0).status()).isEqualTo(InstallmentStatus.NEXT);
            assertThat(response.paymentPlan().installments().get(1).status()).isEqualTo(InstallmentStatus.PENDING);

            // Verify customer credit deduction
            assertThat(customer.getAvailableCreditLineAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));

            // Verify saved entity
            ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
            verify(loanRepository).save(captor.capture());
            LoanEntity savedLoan = captor.getValue();
            assertThat(savedLoan.getExternalId()).isNotNull();
            assertThat(savedLoan.getCustomer()).isEqualTo(customer);
            assertThat(savedLoan.getAmount()).isEqualByComparingTo(requestAmount);
            assertThat(savedLoan.getCommissionAmount()).isEqualByComparingTo("120.00");
            assertThat(savedLoan.getTotalAmount()).isEqualByComparingTo("1120.00");
            assertThat(savedLoan.getSchemeName()).isEqualTo(resolvedScheme);
            assertThat(savedLoan.getInterestRate()).isEqualByComparingTo(resolvedScheme.getInterestRate());
            assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(savedLoan.getInstallments()).hasSize(5);
            assertThat(savedLoan.getInstallments().get(0).getLoan()).isEqualTo(savedLoan);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundWhenCustomerMissing() {
            UUID customerId = UUID.randomUUID();
            LoanRequest request = new LoanRequest(customerId, new BigDecimal("500.00"));

            when(customerRepository.findByExternalId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(customerId.toString());

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidLoanRequestException when amount exceeds available credit line")
        void shouldThrowWhenAmountExceedsAvailableCredit() {
            UUID customerId = UUID.randomUUID();
            CustomerEntity customer = CustomerEntity.builder()
                    .id(1L)
                    .externalId(customerId)
                    .availableCreditLineAmount(new BigDecimal("300.00"))
                    .creditLineAmount(new BigDecimal("1000.00"))
                    .build();

            LoanRequest request = new LoanRequest(customerId, new BigDecimal("500.00"));

            when(customerRepository.findByExternalId(customerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(InvalidLoanRequestException.class)
                    .hasMessageContaining("Requested amount exceeds available credit line");

            assertThat(customer.getAvailableCreditLineAmount()).isEqualByComparingTo("300.00");
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidLoanRequestException when request is null")
        void shouldThrowWhenRequestIsNull() {
            assertThatThrownBy(() -> loanService.createLoan(null))
                    .isInstanceOf(InvalidLoanRequestException.class)
                    .hasMessageContaining("Loan request cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidLoanRequestException when customerId is null")
        void shouldThrowWhenCustomerIdIsNull() {
            LoanRequest request = new LoanRequest(null, new BigDecimal("100.00"));

            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(InvalidLoanRequestException.class)
                    .hasMessageContaining("Customer ID is required");
        }

        @Test
        @DisplayName("Should throw InvalidLoanRequestException when amount is zero or negative")
        void shouldThrowWhenAmountIsZeroOrNegative() {
            UUID customerId = UUID.randomUUID();
            LoanRequest zeroRequest = new LoanRequest(customerId, BigDecimal.ZERO);

            assertThatThrownBy(() -> loanService.createLoan(zeroRequest))
                    .isInstanceOf(InvalidLoanRequestException.class)
                    .hasMessageContaining("Requested amount must be greater than zero");

            LoanRequest negativeRequest = new LoanRequest(customerId, new BigDecimal("-50.00"));

            assertThatThrownBy(() -> loanService.createLoan(negativeRequest))
                    .isInstanceOf(InvalidLoanRequestException.class)
                    .hasMessageContaining("Requested amount must be greater than zero");
        }
    }

    @Nested
    @DisplayName("getLoanByExternalId Tests")
    class GetLoanByExternalIdTests {

        @Test
        @DisplayName("Should return loan response when loan exists")
        void shouldReturnLoanResponseWhenFound() {
            UUID loanId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Instant now = Instant.now();

            CustomerEntity customer = CustomerEntity.builder()
                    .id(1L)
                    .externalId(customerId)
                    .build();

            LoanEntity loan = LoanEntity.builder()
                    .id(100L)
                    .externalId(loanId)
                    .customer(customer)
                    .amount(new BigDecimal("1000.00"))
                    .commissionAmount(new BigDecimal("120.00"))
                    .totalAmount(new BigDecimal("1120.00"))
                    .schemeName(PaymentScheme.SCHEME_1)
                    .interestRate(PaymentScheme.SCHEME_1.getInterestRate())
                    .status(LoanStatus.ACTIVE)
                    .createdAt(now)
                    .build();

            when(loanRepository.findByExternalId(loanId)).thenReturn(Optional.of(loan));

            LoanResponse response = loanService.getLoanByExternalId(loanId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(loanId);
            assertThat(response.customerId()).isEqualTo(customerId);
            assertThat(response.amount()).isEqualByComparingTo("1000.00");
            assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(response.createdAt()).isEqualTo(now);
            verify(loanRepository).findByExternalId(loanId);
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan does not exist")
        void shouldThrowLoanNotFoundWhenMissing() {
            UUID loanId = UUID.randomUUID();
            when(loanRepository.findByExternalId(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoanByExternalId(loanId))
                    .isInstanceOf(LoanNotFoundException.class)
                    .hasMessageContaining(loanId.toString());

            verify(loanRepository).findByExternalId(loanId);
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loanId is null")
        void shouldThrowWhenLoanIdIsNull() {
            assertThatThrownBy(() -> loanService.getLoanByExternalId(null))
                    .isInstanceOf(LoanNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should support default 3-argument constructor")
        void testDefaultConstructor() {
            LoanService serviceWithDefaultClock = new LoanService(loanRepository, customerRepository, paymentSchemeResolver);
            assertThat(serviceWithDefaultClock).isNotNull();
        }
    }
}
