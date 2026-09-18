package com.aplazo.bnpl.dto;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.dto.response.ErrorResponse;
import com.aplazo.bnpl.dto.response.InstallmentResponse;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.dto.response.PaymentPlanResponse;
import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.LoanStatus;
import com.aplazo.bnpl.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Validation and Serialization Tests")
class DtoValidationTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("CustomerRequest Validation")
    class CustomerRequestValidationTests {

        @Test
        @DisplayName("Valid CustomerRequest passes validation")
        void testValidCustomerRequest() {
            CustomerRequest request = new CustomerRequest(
                    "Pepe",
                    "García",
                    "Flores",
                    LocalDate.of(1998, 7, 21)
            );

            Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank fields and null date fail validation")
        void testInvalidCustomerRequest() {
            CustomerRequest request = new CustomerRequest("", "  ", "", null);

            Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(4);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("firstName", "lastName", "secondLastName", "dateOfBirth");
        }

        @Test
        @DisplayName("Deserialization supports secondLastNme alias from OpenAPI examples")
        void testAliasDeserialization() throws Exception {
            String json = """
                    {
                        "firstName": "Pepe",
                        "lastName": "García",
                        "secondLastNme": "Flores",
                        "dateOfBirth": "1998-07-21"
                    }
                    """;

            CustomerRequest request = objectMapper.readValue(json, CustomerRequest.class);
            assertThat(request.firstName()).isEqualTo("Pepe");
            assertThat(request.lastName()).isEqualTo("García");
            assertThat(request.secondLastName()).isEqualTo("Flores");
            assertThat(request.dateOfBirth()).isEqualTo(LocalDate.of(1998, 7, 21));
        }
    }

    @Nested
    @DisplayName("LoanRequest Validation")
    class LoanRequestValidationTests {

        @Test
        @DisplayName("Valid LoanRequest passes validation")
        void testValidLoanRequest() {
            LoanRequest request = new LoanRequest(
                    UUID.randomUUID(),
                    BigDecimal.valueOf(500.00)
            );

            Set<ConstraintViolation<LoanRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Null customerId and zero or negative amount fail validation")
        void testInvalidLoanRequest() {
            LoanRequest nullAndZero = new LoanRequest(null, BigDecimal.ZERO);
            Set<ConstraintViolation<LoanRequest>> violationsZero = validator.validate(nullAndZero);
            assertThat(violationsZero).hasSize(2);

            LoanRequest negativeAmount = new LoanRequest(UUID.randomUUID(), BigDecimal.valueOf(-100));
            Set<ConstraintViolation<LoanRequest>> violationsNegative = validator.validate(negativeAmount);
            assertThat(violationsNegative).hasSize(1);
            assertThat(violationsNegative.iterator().next().getPropertyPath().toString()).isEqualTo("amount");
        }
    }

    @Nested
    @DisplayName("DTO Response Serialization")
    class DtoResponseSerializationTests {

        @Test
        @DisplayName("CustomerResponse serializes cleanly")
        void testCustomerResponseSerialization() throws Exception {
            UUID id = UUID.randomUUID();
            Instant now = Instant.parse("2026-09-18T10:00:00Z");
            CustomerResponse response = new CustomerResponse(
                    id,
                    BigDecimal.valueOf(1000.00),
                    BigDecimal.valueOf(1000.00),
                    now
            );

            String json = objectMapper.writeValueAsString(response);
            assertThat(json).contains(id.toString());
            assertThat(json).contains("1000.0");
            assertThat(json).contains("2026-09-18T10:00:00Z");
        }

        @Test
        @DisplayName("LoanResponse with PaymentPlanResponse serializes cleanly")
        void testLoanResponseSerialization() throws Exception {
            UUID loanId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Instant now = Instant.parse("2026-09-18T10:00:00Z");

            List<InstallmentResponse> installments = List.of(
                    new InstallmentResponse(BigDecimal.valueOf(100.00), LocalDate.of(2026, 9, 25), InstallmentStatus.NEXT),
                    new InstallmentResponse(BigDecimal.valueOf(100.00), LocalDate.of(2026, 10, 2), InstallmentStatus.PENDING)
            );
            PaymentPlanResponse paymentPlan = new PaymentPlanResponse(BigDecimal.valueOf(50.00), installments);

            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    customerId,
                    BigDecimal.valueOf(500.00),
                    LoanStatus.ACTIVE,
                    now,
                    paymentPlan
            );

            String json = objectMapper.writeValueAsString(loanResponse);
            assertThat(json).contains(loanId.toString());
            assertThat(json).contains(customerId.toString());
            assertThat(json).contains("ACTIVE");
            assertThat(json).contains("NEXT");
            assertThat(json).contains("PENDING");
            assertThat(json).contains("2026-09-25");
        }

        @Test
        @DisplayName("ErrorResponse of() factory methods construct expected payload")
        void testErrorResponseOf() {
            ErrorResponse response = ErrorResponse.of(
                    ErrorCode.INVALID_CUSTOMER_REQUEST,
                    "Customer must be at least 18",
                    "/v1/customers"
            );

            assertThat(response.code()).isEqualTo("APZ000002");
            assertThat(response.error()).isEqualTo("INVALID_CUSTOMER_REQUEST");
            assertThat(response.message()).isEqualTo("Customer must be at least 18");
            assertThat(response.path()).isEqualTo("/v1/customers");
            assertThat(response.timestamp()).isGreaterThan(0);
        }
    }
}
