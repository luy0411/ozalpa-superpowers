package com.aplazo.bnpl.service;

import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.entity.CustomerEntity;
import com.aplazo.bnpl.entity.InstallmentEntity;
import com.aplazo.bnpl.entity.LoanEntity;
import com.aplazo.bnpl.entity.LoanStatus;
import com.aplazo.bnpl.entity.PaymentScheme;
import com.aplazo.bnpl.exception.CustomerNotFoundException;
import com.aplazo.bnpl.exception.InvalidLoanRequestException;
import com.aplazo.bnpl.exception.LoanNotFoundException;
import com.aplazo.bnpl.mapper.LoanMapper;
import com.aplazo.bnpl.repository.CustomerRepository;
import com.aplazo.bnpl.repository.LoanRepository;
import com.aplazo.bnpl.service.model.PaymentPlanCalculation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final PaymentSchemeResolver paymentSchemeResolver;
    private final Clock clock;

    @Autowired
    public LoanService(LoanRepository loanRepository,
                       CustomerRepository customerRepository,
                       PaymentSchemeResolver paymentSchemeResolver) {
        this(loanRepository, customerRepository, paymentSchemeResolver, Clock.systemDefaultZone());
    }

    public LoanService(LoanRepository loanRepository,
                       CustomerRepository customerRepository,
                       PaymentSchemeResolver paymentSchemeResolver,
                       Clock clock) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.paymentSchemeResolver = paymentSchemeResolver;
        this.clock = clock;
    }

    @Transactional
    public LoanResponse createLoan(LoanRequest request) {
        if (request == null) {
            throw new InvalidLoanRequestException("Loan request cannot be null");
        }
        if (request.customerId() == null) {
            throw new InvalidLoanRequestException("Customer ID is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoanRequestException("Requested amount must be greater than zero");
        }

        CustomerEntity customer = customerRepository.findByExternalId(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));

        BigDecimal requestedAmount = request.amount();
        BigDecimal availableCredit = customer.getAvailableCreditLineAmount();

        if (availableCredit == null || requestedAmount.compareTo(availableCredit) > 0) {
            throw new InvalidLoanRequestException("Requested amount exceeds available credit line");
        }

        // Deduct available credit atomically
        customer.setAvailableCreditLineAmount(availableCredit.subtract(requestedAmount));

        // Resolve scheme and calculate payment plan
        PaymentScheme scheme = paymentSchemeResolver.resolveScheme(customer.getFirstName(), customer.getId());
        LocalDate purchaseDate = LocalDate.now(clock);
        PaymentPlanCalculation planCalculation = paymentSchemeResolver.calculatePlan(requestedAmount, scheme, purchaseDate);

        LoanEntity loanEntity = LoanEntity.builder()
                .externalId(UUID.randomUUID())
                .customer(customer)
                .amount(requestedAmount)
                .commissionAmount(planCalculation.commissionAmount())
                .totalAmount(planCalculation.totalAmount())
                .schemeName(scheme)
                .interestRate(scheme.getInterestRate())
                .status(LoanStatus.ACTIVE)
                .createdAt(Instant.now(clock))
                .build();

        List<InstallmentEntity> installmentEntities = new ArrayList<>();
        planCalculation.installments().forEach(calc -> {
            InstallmentEntity installment = InstallmentEntity.builder()
                    .loan(loanEntity)
                    .installmentNumber(calc.installmentNumber())
                    .amount(calc.amount())
                    .scheduledPaymentDate(calc.scheduledPaymentDate())
                    .status(calc.status())
                    .createdAt(Instant.now(clock))
                    .build();
            installmentEntities.add(installment);
        });

        loanEntity.setInstallments(installmentEntities);

        LoanEntity savedLoan = loanRepository.save(loanEntity);

        return LoanMapper.toResponse(savedLoan, planCalculation);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanByExternalId(UUID loanId) {
        if (loanId == null) {
            throw new LoanNotFoundException((UUID) null);
        }

        return loanRepository.findByExternalId(loanId)
                .map(LoanMapper::toResponse)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }
}
