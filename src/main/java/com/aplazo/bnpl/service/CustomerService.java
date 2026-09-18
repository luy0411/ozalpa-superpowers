package com.aplazo.bnpl.service;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.entity.CustomerEntity;
import com.aplazo.bnpl.exception.CustomerNotFoundException;
import com.aplazo.bnpl.exception.InvalidCustomerRequestException;
import com.aplazo.bnpl.mapper.CustomerMapper;
import com.aplazo.bnpl.repository.CustomerRepository;
import com.aplazo.bnpl.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditLineCalculator creditLineCalculator;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           CreditLineCalculator creditLineCalculator,
                           JwtTokenProvider jwtTokenProvider) {
        this(customerRepository, creditLineCalculator, jwtTokenProvider, Clock.systemDefaultZone());
    }

    public CustomerService(CustomerRepository customerRepository,
                           CreditLineCalculator creditLineCalculator,
                           JwtTokenProvider jwtTokenProvider,
                           Clock clock) {
        this.customerRepository = customerRepository;
        this.creditLineCalculator = creditLineCalculator;
        this.jwtTokenProvider = jwtTokenProvider;
        this.clock = clock;
    }

    @Transactional
    public CustomerRegistrationResult createCustomer(CustomerRequest request) {
        if (request == null) {
            throw new InvalidCustomerRequestException("Customer request cannot be null");
        }
        if (request.dateOfBirth() == null) {
            throw new InvalidCustomerRequestException("Customer birth date cannot be null");
        }

        LocalDate referenceDate = LocalDate.now(clock);
        BigDecimal creditLineAmount = creditLineCalculator.calculateCreditLine(request.dateOfBirth(), referenceDate);

        CustomerEntity customerEntity = CustomerEntity.builder()
                .externalId(UUID.randomUUID())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .secondLastName(request.secondLastName())
                .dateOfBirth(request.dateOfBirth())
                .creditLineAmount(creditLineAmount)
                .availableCreditLineAmount(creditLineAmount)
                .createdAt(Instant.now(clock))
                .build();

        CustomerEntity savedCustomer = customerRepository.save(customerEntity);

        String token = jwtTokenProvider.generateToken(savedCustomer.getExternalId());
        CustomerResponse customerResponse = CustomerMapper.toResponse(savedCustomer);

        return new CustomerRegistrationResult(customerResponse, token);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByExternalId(UUID customerId) {
        if (customerId == null) {
            throw new CustomerNotFoundException((UUID) null);
        }

        return customerRepository.findByExternalId(customerId)
                .map(CustomerMapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
