package com.aplazo.bnpl.controller;

import com.aplazo.bnpl.dto.request.CustomerRequest;
import com.aplazo.bnpl.dto.response.CustomerResponse;
import com.aplazo.bnpl.exception.CustomerNotFoundException;
import com.aplazo.bnpl.exception.GlobalExceptionHandler;
import com.aplazo.bnpl.exception.InvalidCustomerRequestException;
import com.aplazo.bnpl.security.CustomAuthenticationEntryPoint;
import com.aplazo.bnpl.security.JwtAuthenticationFilter;
import com.aplazo.bnpl.security.JwtTokenProvider;
import com.aplazo.bnpl.security.SecurityConfig;
import com.aplazo.bnpl.service.CustomerRegistrationResult;
import com.aplazo.bnpl.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
@DisplayName("CustomerController Tests")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomerService customerService;

    @Nested
    @DisplayName("POST /v1/customers Tests")
    class CreateCustomerEndpointTests {

        @Test
        @DisplayName("Should return 201 Created with Location, X-Auth-Token and body for valid customer (public endpoint)")
        void shouldCreateCustomerSuccessfully() throws Exception {
            UUID customerId = UUID.randomUUID();
            LocalDate dob = LocalDate.of(1995, 8, 14);
            Instant createdAt = Instant.parse("2026-09-18T10:15:30.00Z");
            String token = "jwt.sample.token";

            CustomerRequest request = new CustomerRequest("Juan", "Lopez", "Perez", dob);
            CustomerResponse customerResponse = new CustomerResponse(
                    customerId,
                    new BigDecimal("5000.00"),
                    new BigDecimal("5000.00"),
                    createdAt
            );
            CustomerRegistrationResult result = new CustomerRegistrationResult(customerResponse, token);

            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(result);

            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/v1/customers/" + customerId))
                    .andExpect(header().string("X-Auth-Token", token))
                    .andExpect(jsonPath("$.id", is(customerId.toString())))
                    .andExpect(jsonPath("$.creditLineAmount", is(5000.00)))
                    .andExpect(jsonPath("$.availableCreditLineAmount", is(5000.00)))
                    .andExpect(jsonPath("$.createdAt", is(createdAt.toString())));

            verify(customerService).createCustomer(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body fails validation")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            String invalidJson = """
                    {
                        "firstName": "",
                        "lastName": "Lopez",
                        "secondLastName": "Perez"
                    }
                    """;

            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000002")))
                    .andExpect(jsonPath("$.error", is("INVALID_CUSTOMER_REQUEST")))
                    .andExpect(jsonPath("$.message", containsString("firstName")));

            verify(customerService, never()).createCustomer(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when customer age is invalid")
        void shouldReturnBadRequestWhenAgeIsInvalid() throws Exception {
            LocalDate dob = LocalDate.of(2015, 1, 1);
            CustomerRequest request = new CustomerRequest("Minor", "Lopez", "Perez", dob);

            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenThrow(new InvalidCustomerRequestException("Customer must be between 18 and 65 years old"));

            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000002")))
                    .andExpect(jsonPath("$.error", is("INVALID_CUSTOMER_REQUEST")))
                    .andExpect(jsonPath("$.message", containsString("between 18 and 65")));
        }
    }

    @Nested
    @DisplayName("GET /v1/customers/{customerId} Tests")
    class GetCustomerEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with CustomerResponse when authenticated")
        void shouldReturnCustomerWhenAuthenticated() throws Exception {
            UUID customerId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-09-18T10:15:30.00Z");
            CustomerResponse customerResponse = new CustomerResponse(
                    customerId,
                    new BigDecimal("5000.00"),
                    new BigDecimal("4200.00"),
                    createdAt
            );

            when(customerService.getCustomerByExternalId(customerId)).thenReturn(customerResponse);

            String validToken = jwtTokenProvider.generateToken(customerId);

            mockMvc.perform(get("/v1/customers/{customerId}", customerId)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(customerId.toString())))
                    .andExpect(jsonPath("$.creditLineAmount", is(5000.00)))
                    .andExpect(jsonPath("$.availableCreditLineAmount", is(4200.00)))
                    .andExpect(jsonPath("$.createdAt", is(createdAt.toString())));

            verify(customerService).getCustomerByExternalId(customerId);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
        void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
            UUID customerId = UUID.randomUUID();

            mockMvc.perform(get("/v1/customers/{customerId}", customerId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

            verify(customerService, never()).getCustomerByExternalId(any());
        }

        @Test
        @DisplayName("Should return 404 Not Found when customer does not exist")
        void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
            UUID customerId = UUID.randomUUID();
            String validToken = jwtTokenProvider.generateToken(customerId);

            when(customerService.getCustomerByExternalId(customerId))
                    .thenThrow(new CustomerNotFoundException(customerId));

            mockMvc.perform(get("/v1/customers/{customerId}", customerId)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("APZ000005")))
                    .andExpect(jsonPath("$.error", is("CUSTOMER_NOT_FOUND")))
                    .andExpect(jsonPath("$.message", containsString(customerId.toString())));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when customerId is not a valid UUID")
        void shouldReturnBadRequestWhenUuidIsInvalid() throws Exception {
            String validToken = jwtTokenProvider.generateToken(UUID.randomUUID());

            mockMvc.perform(get("/v1/customers/invalid-uuid-format")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000004")))
                    .andExpect(jsonPath("$.error", is("INVALID_REQUEST")));
        }
    }
}
