package com.aplazo.bnpl.exception;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.request.LoanRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/v1")
    static class DummyController {

        @PostMapping("/customers")
        public String createCustomer(@Valid @RequestBody CustomerRequest request) {
            return "customer-created";
        }

        @GetMapping("/customers/{customerId}")
        public String getCustomer(@PathVariable UUID customerId) {
            if (customerId.equals(UUID.fromString("00000000-0000-0000-0000-000000000001"))) {
                throw new CustomerNotFoundException(customerId);
            }
            if (customerId.equals(UUID.fromString("00000000-0000-0000-0000-000000000002"))) {
                throw new InvalidCustomerRequestException("Customer age out of range");
            }
            return "customer-found";
        }

        @PostMapping("/loans")
        public String createLoan(@Valid @RequestBody LoanRequest request) {
            if (request.customerId().equals(UUID.fromString("00000000-0000-0000-0000-000000000003"))) {
                throw new InvalidLoanRequestException("Requested amount exceeds available credit line");
            }
            return "loan-created";
        }

        @GetMapping("/loans/{loanId}")
        public String getLoan(@PathVariable UUID loanId) {
            if (loanId.equals(UUID.fromString("00000000-0000-0000-0000-000000000004"))) {
                throw new LoanNotFoundException(loanId);
            }
            return "loan-found";
        }

        @GetMapping("/error-trigger")
        public String triggerInternalError() {
            throw new RuntimeException("Unexpected database error");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Customer Error Mappings")
    class CustomerErrorTests {

        @Test
        @DisplayName("InvalidCustomerRequestException -> 400 APZ000002 INVALID_CUSTOMER_REQUEST")
        void testInvalidCustomerRequestException() throws Exception {
            mockMvc.perform(get("/v1/customers/00000000-0000-0000-0000-000000000002"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000002")))
                    .andExpect(jsonPath("$.error", is("INVALID_CUSTOMER_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", is("Customer age out of range")))
                    .andExpect(jsonPath("$.path", is("/v1/customers/00000000-0000-0000-0000-000000000002")));
        }

        @Test
        @DisplayName("Validation failure on CustomerRequest -> 400 APZ000002 INVALID_CUSTOMER_REQUEST")
        void testCustomerValidationFailure() throws Exception {
            String invalidBody = """
                    {
                        "firstName": "",
                        "lastName": "García",
                        "secondLastName": "Flores",
                        "dateOfBirth": null
                    }
                    """;

            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000002")))
                    .andExpect(jsonPath("$.error", is("INVALID_CUSTOMER_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.path", is("/v1/customers")))
                    .andExpect(jsonPath("$.message", containsString("firstName")))
                    .andExpect(jsonPath("$.message", containsString("dateOfBirth")));
        }

        @Test
        @DisplayName("Malformed JSON on customers endpoint -> 400 APZ000002 INVALID_CUSTOMER_REQUEST")
        void testMalformedJsonCustomerRequest() throws Exception {
            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not_valid_json}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000002")))
                    .andExpect(jsonPath("$.error", is("INVALID_CUSTOMER_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.path", is("/v1/customers")));
        }

        @Test
        @DisplayName("CustomerNotFoundException -> 404 APZ000005 CUSTOMER_NOT_FOUND")
        void testCustomerNotFoundException() throws Exception {
            mockMvc.perform(get("/v1/customers/00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("APZ000005")))
                    .andExpect(jsonPath("$.error", is("CUSTOMER_NOT_FOUND")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("Customer not found with id: 00000000-0000-0000-0000-000000000001")))
                    .andExpect(jsonPath("$.path", is("/v1/customers/00000000-0000-0000-0000-000000000001")));
        }
    }

    @Nested
    @DisplayName("Loan Error Mappings")
    class LoanErrorTests {

        @Test
        @DisplayName("InvalidLoanRequestException -> 400 APZ000006 INVALID_LOAN_REQUEST")
        void testInvalidLoanRequestException() throws Exception {
            String body = """
                    {
                        "customerId": "00000000-0000-0000-0000-000000000003",
                        "amount": 5000.00
                    }
                    """;

            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000006")))
                    .andExpect(jsonPath("$.error", is("INVALID_LOAN_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", is("Requested amount exceeds available credit line")))
                    .andExpect(jsonPath("$.path", is("/v1/loans")));
        }

        @Test
        @DisplayName("Validation failure on LoanRequest -> 400 APZ000006 INVALID_LOAN_REQUEST")
        void testLoanValidationFailure() throws Exception {
            String invalidBody = """
                    {
                        "customerId": null,
                        "amount": -10.00
                    }
                    """;

            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000006")))
                    .andExpect(jsonPath("$.error", is("INVALID_LOAN_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.path", is("/v1/loans")))
                    .andExpect(jsonPath("$.message", containsString("customerId")))
                    .andExpect(jsonPath("$.message", containsString("amount")));
        }

        @Test
        @DisplayName("Malformed JSON on loans endpoint -> 400 APZ000006 INVALID_LOAN_REQUEST")
        void testMalformedJsonLoanRequest() throws Exception {
            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-payload}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000006")))
                    .andExpect(jsonPath("$.error", is("INVALID_LOAN_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.path", is("/v1/loans")));
        }

        @Test
        @DisplayName("LoanNotFoundException -> 404 APZ000008 LOAN_NOT_FOUND")
        void testLoanNotFoundException() throws Exception {
            mockMvc.perform(get("/v1/loans/00000000-0000-0000-0000-000000000004"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("APZ000008")))
                    .andExpect(jsonPath("$.error", is("LOAN_NOT_FOUND")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("Loan not found with id: 00000000-0000-0000-0000-000000000004")))
                    .andExpect(jsonPath("$.path", is("/v1/loans/00000000-0000-0000-0000-000000000004")));
        }
    }

    @Nested
    @DisplayName("General and Parameter Error Mappings")
    class GeneralErrorTests {

        @Test
        @DisplayName("MethodArgumentTypeMismatchException -> 400 APZ000004 INVALID_REQUEST")
        void testInvalidUuidParameter() throws Exception {
            mockMvc.perform(get("/v1/customers/invalid-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000004")))
                    .andExpect(jsonPath("$.error", is("INVALID_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("Failed to convert parameter 'customerId'")))
                    .andExpect(jsonPath("$.path", is("/v1/customers/invalid-uuid")));
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException on loan path -> 400 APZ000004 INVALID_REQUEST")
        void testInvalidLoanUuidParameter() throws Exception {
            mockMvc.perform(get("/v1/loans/not-a-valid-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000004")))
                    .andExpect(jsonPath("$.error", is("INVALID_REQUEST")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("Failed to convert parameter 'loanId'")))
                    .andExpect(jsonPath("$.path", is("/v1/loans/not-a-valid-uuid")));
        }

        @Test
        @DisplayName("Unhandled Exception -> 500 APZ000001 INTERNAL_SERVER_ERROR")
        void testInternalServerError() throws Exception {
            mockMvc.perform(get("/v1/error-trigger"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code", is("APZ000001")))
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", is("Unexpected database error")))
                    .andExpect(jsonPath("$.path", is("/v1/error-trigger")));
        }
    }

    @Nested
    @DisplayName("Direct Handler and Edge Cases")
    class DirectHandlerTests {

        private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

        @Test
        @DisplayName("Direct handler methods handle null request gracefully")
        void testNullRequestHandling() {
            var resCustomer = handler.handleCustomerNotFoundException(new CustomerNotFoundException("not found"), null);
            org.assertj.core.api.Assertions.assertThat(resCustomer.getStatusCode().value()).isEqualTo(404);
            org.assertj.core.api.Assertions.assertThat(resCustomer.getBody().path()).isEmpty();

            var resLoan = handler.handleLoanNotFoundException(new LoanNotFoundException("not found"), null);
            org.assertj.core.api.Assertions.assertThat(resLoan.getStatusCode().value()).isEqualTo(404);

            var resGeneric = handler.handleGenericException(new RuntimeException((String) null), null);
            org.assertj.core.api.Assertions.assertThat(resGeneric.getStatusCode().value()).isEqualTo(500);
            org.assertj.core.api.Assertions.assertThat(resGeneric.getBody().message()).isEqualTo("Internal server error");
        }

        @Test
        @DisplayName("ConstraintViolationException handling")
        void testConstraintViolationException() {
            jakarta.validation.ConstraintViolationException ex =
                    new jakarta.validation.ConstraintViolationException("field is invalid", java.util.Collections.emptySet());
            org.springframework.mock.web.MockHttpServletRequest request =
                    new org.springframework.mock.web.MockHttpServletRequest("POST", "/v1/customers");

            var res = handler.handleConstraintViolationException(ex, request);
            org.assertj.core.api.Assertions.assertThat(res.getStatusCode().value()).isEqualTo(400);
            org.assertj.core.api.Assertions.assertThat(res.getBody().code()).isEqualTo("APZ000002");
        }

        @Test
        @DisplayName("Exception constructors check")
        void testExceptionConstructors() {
            UUID id = UUID.randomUUID();
            CustomerNotFoundException cnfe1 = new CustomerNotFoundException(id);
            org.assertj.core.api.Assertions.assertThat(cnfe1.getCustomerId()).isEqualTo(id);
            CustomerNotFoundException cnfe2 = new CustomerNotFoundException(id, "custom msg");
            org.assertj.core.api.Assertions.assertThat(cnfe2.getMessage()).isEqualTo("custom msg");

            LoanNotFoundException lnfe1 = new LoanNotFoundException(id);
            org.assertj.core.api.Assertions.assertThat(lnfe1.getLoanId()).isEqualTo(id);
            LoanNotFoundException lnfe2 = new LoanNotFoundException(id, "custom msg");
            org.assertj.core.api.Assertions.assertThat(lnfe2.getMessage()).isEqualTo("custom msg");

            InvalidLoanRequestException ilre = new InvalidLoanRequestException("error", new RuntimeException("cause"));
            org.assertj.core.api.Assertions.assertThat(ilre.getCause()).isNotNull();

            InvalidCustomerRequestException icre = new InvalidCustomerRequestException("error", new RuntimeException("cause"));
            org.assertj.core.api.Assertions.assertThat(icre.getCause()).isNotNull();
        }
    }
}
