package com.aplazo.bnpl.service;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.entity.CustomerEntity;
import com.aplazo.bnpl.exception.CustomerNotFoundException;
import com.aplazo.bnpl.exception.InvalidCustomerRequestException;
import com.aplazo.bnpl.repository.CustomerRepository;
import com.aplazo.bnpl.security.JwtTokenProvider;
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
@DisplayName("CustomerService Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreditLineCalculator creditLineCalculator;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private Clock fixedClock;
    private CustomerService customerService;

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        customerService = new CustomerService(customerRepository, creditLineCalculator, jwtTokenProvider, fixedClock);
    }

    @Nested
    @DisplayName("createCustomer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should successfully create customer, calculate credit limit, persist entity and generate JWT")
        void shouldCreateCustomerSuccessfully() {
            LocalDate dob = LocalDate.of(2000, 1, 1);
            CustomerRequest request = new CustomerRequest("Pepe", "Garcia", "Flores", dob);
            BigDecimal expectedLimit = new BigDecimal("3000.00");
            String expectedToken = "jwt-mock-token-abc";

            when(creditLineCalculator.calculateCreditLine(eq(dob), eq(FIXED_TODAY))).thenReturn(expectedLimit);
            when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
                CustomerEntity entity = invocation.getArgument(0);
                return CustomerEntity.builder()
                        .id(1L)
                        .externalId(entity.getExternalId())
                        .firstName(entity.getFirstName())
                        .lastName(entity.getLastName())
                        .secondLastName(entity.getSecondLastName())
                        .dateOfBirth(entity.getDateOfBirth())
                        .creditLineAmount(entity.getCreditLineAmount())
                        .availableCreditLineAmount(entity.getAvailableCreditLineAmount())
                        .createdAt(Instant.now())
                        .build();
            });
            when(jwtTokenProvider.generateToken(any(UUID.class))).thenReturn(expectedToken);

            CustomerRegistrationResult result = customerService.createCustomer(request);

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo(expectedToken);
            assertThat(result.customerResponse()).isNotNull();
            assertThat(result.customer()).isNotNull();
            assertThat(result.customerResponse().id()).isNotNull();
            assertThat(result.customerResponse().creditLineAmount()).isEqualByComparingTo(expectedLimit);
            assertThat(result.customerResponse().availableCreditLineAmount()).isEqualByComparingTo(expectedLimit);
            assertThat(result.customerResponse().createdAt()).isNotNull();

            ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
            verify(customerRepository).save(captor.capture());
            CustomerEntity savedEntity = captor.getValue();
            assertThat(savedEntity.getExternalId()).isNotNull();
            assertThat(savedEntity.getFirstName()).isEqualTo("Pepe");
            assertThat(savedEntity.getLastName()).isEqualTo("Garcia");
            assertThat(savedEntity.getSecondLastName()).isEqualTo("Flores");
            assertThat(savedEntity.getDateOfBirth()).isEqualTo(dob);
            assertThat(savedEntity.getCreditLineAmount()).isEqualByComparingTo(expectedLimit);
            assertThat(savedEntity.getAvailableCreditLineAmount()).isEqualByComparingTo(expectedLimit);

            verify(jwtTokenProvider).generateToken(savedEntity.getExternalId());
        }

        @Test
        @DisplayName("Should reject customer under 18 with InvalidCustomerRequestException")
        void shouldRejectCustomerUnder18() {
            LocalDate dob = LocalDate.of(2010, 1, 1);
            CustomerRequest request = new CustomerRequest("Minor", "User", "Test", dob);

            when(creditLineCalculator.calculateCreditLine(eq(dob), eq(FIXED_TODAY)))
                    .thenThrow(new InvalidCustomerRequestException("Customer must be between 18 and 65 years old"));

            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(InvalidCustomerRequestException.class)
                    .hasMessageContaining("Customer must be between 18 and 65 years old");

            verify(customerRepository, never()).save(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should reject customer over 65 with InvalidCustomerRequestException")
        void shouldRejectCustomerOver65() {
            LocalDate dob = LocalDate.of(1950, 1, 1);
            CustomerRequest request = new CustomerRequest("Senior", "User", "Test", dob);

            when(creditLineCalculator.calculateCreditLine(eq(dob), eq(FIXED_TODAY)))
                    .thenThrow(new InvalidCustomerRequestException("Customer must be between 18 and 65 years old"));

            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(InvalidCustomerRequestException.class)
                    .hasMessageContaining("Customer must be between 18 and 65 years old");

            verify(customerRepository, never()).save(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw InvalidCustomerRequestException when request is null")
        void shouldThrowWhenRequestIsNull() {
            assertThatThrownBy(() -> customerService.createCustomer(null))
                    .isInstanceOf(InvalidCustomerRequestException.class)
                    .hasMessageContaining("cannot be null");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidCustomerRequestException when date of birth is null")
        void shouldThrowWhenDateOfBirthIsNull() {
            CustomerRequest request = new CustomerRequest("No", "Birth", "Date", null);

            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(InvalidCustomerRequestException.class);

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCustomerByExternalId Tests")
    class GetCustomerByExternalIdTests {

        @Test
        @DisplayName("Should return customer response when customer exists")
        void shouldReturnCustomerResponseWhenFound() {
            UUID customerId = UUID.randomUUID();
            Instant now = Instant.now();
            CustomerEntity entity = CustomerEntity.builder()
                    .id(10L)
                    .externalId(customerId)
                    .firstName("Maria")
                    .lastName("Lopez")
                    .secondLastName("Hernandez")
                    .dateOfBirth(LocalDate.of(1995, 5, 20))
                    .creditLineAmount(new BigDecimal("5000.00"))
                    .availableCreditLineAmount(new BigDecimal("4200.00"))
                    .createdAt(now)
                    .build();

            when(customerRepository.findByExternalId(customerId)).thenReturn(Optional.of(entity));

            CustomerResponse response = customerService.getCustomerByExternalId(customerId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(customerId);
            assertThat(response.creditLineAmount()).isEqualByComparingTo("5000.00");
            assertThat(response.availableCreditLineAmount()).isEqualByComparingTo("4200.00");
            assertThat(response.createdAt()).isEqualTo(now);
            verify(customerRepository).findByExternalId(customerId);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer is not found")
        void shouldThrowCustomerNotFoundWhenNotFound() {
            UUID customerId = UUID.randomUUID();
            when(customerRepository.findByExternalId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerByExternalId(customerId))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(customerId.toString());

            verify(customerRepository).findByExternalId(customerId);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customerId is null")
        void shouldThrowWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.getCustomerByExternalId(null))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Constructor and default Clock tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should support default 3-argument constructor")
        void testDefaultConstructor() {
            CustomerService serviceWithDefaultClock = new CustomerService(customerRepository, creditLineCalculator, jwtTokenProvider);
            assertThat(serviceWithDefaultClock).isNotNull();
        }
    }
}
