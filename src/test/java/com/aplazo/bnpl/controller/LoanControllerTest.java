package com.aplazo.bnpl.controller;

import com.aplazo.bnpl.dto.request.LoanRequest;
import com.aplazo.bnpl.dto.response.InstallmentResponse;
import com.aplazo.bnpl.dto.response.LoanResponse;
import com.aplazo.bnpl.dto.response.PaymentPlanResponse;
import com.aplazo.bnpl.entity.InstallmentStatus;
import com.aplazo.bnpl.entity.LoanStatus;
import com.aplazo.bnpl.exception.GlobalExceptionHandler;
import com.aplazo.bnpl.exception.InvalidLoanRequestException;
import com.aplazo.bnpl.exception.LoanNotFoundException;
import com.aplazo.bnpl.security.CustomAuthenticationEntryPoint;
import com.aplazo.bnpl.security.JwtAuthenticationFilter;
import com.aplazo.bnpl.security.JwtTokenProvider;
import com.aplazo.bnpl.security.SecurityConfig;
import com.aplazo.bnpl.service.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
@DisplayName("LoanController Tests")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private LoanService loanService;

    private String getValidBearerToken(UUID customerId) {
        return "Bearer " + jwtTokenProvider.generateToken(customerId);
    }

    private String getValidXAuthToken(UUID customerId) {
        return jwtTokenProvider.generateToken(customerId);
    }

    @Nested
    @DisplayName("POST /v1/loans Tests")
    class CreateLoanEndpointTests {

        @Test
        @DisplayName("Should return 201 Created with Location header and body when authenticated with Bearer token")
        void shouldCreateLoanSuccessfullyWithBearerToken() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID loanId = UUID.randomUUID();
            Instant now = Instant.parse("2026-09-18T12:00:00Z");

            LoanRequest request = new LoanRequest(customerId, new BigDecimal("400.80"));

            List<InstallmentResponse> installments = List.of(
                    new InstallmentResponse(new BigDecimal("92.98"), LocalDate.of(2026, 10, 2), InstallmentStatus.NEXT),
                    new InstallmentResponse(new BigDecimal("92.98"), LocalDate.of(2026, 10, 16), InstallmentStatus.PENDING),
                    new InstallmentResponse(new BigDecimal("92.98"), LocalDate.of(2026, 10, 30), InstallmentStatus.PENDING),
                    new InstallmentResponse(new BigDecimal("92.98"), LocalDate.of(2026, 11, 13), InstallmentStatus.PENDING),
                    new InstallmentResponse(new BigDecimal("92.98"), LocalDate.of(2026, 11, 27), InstallmentStatus.PENDING)
            );
            PaymentPlanResponse paymentPlan = new PaymentPlanResponse(new BigDecimal("64.12"), installments);

            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    customerId,
                    new BigDecimal("400.80"),
                    LoanStatus.ACTIVE,
                    now,
                    paymentPlan
            );

            when(loanService.createLoan(any(LoanRequest.class))).thenReturn(loanResponse);

            mockMvc.perform(post("/v1/loans")
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/v1/loans/" + loanId))
                    .andExpect(jsonPath("$.id", is(loanId.toString())))
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.amount", is(400.80)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.createdAt", is(now.toString())))
                    .andExpect(jsonPath("$.paymentPlan.commissionAmount", is(64.12)))
                    .andExpect(jsonPath("$.paymentPlan.installments", hasSize(5)))
                    .andExpect(jsonPath("$.paymentPlan.installments[0].status", is("NEXT")))
                    .andExpect(jsonPath("$.paymentPlan.installments[1].status", is("PENDING")));

            verify(loanService).createLoan(request);
        }

        @Test
        @DisplayName("Should return 201 Created when authenticated with X-Auth-Token header")
        void shouldCreateLoanSuccessfullyWithXAuthToken() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID loanId = UUID.randomUUID();

            LoanRequest request = new LoanRequest(customerId, new BigDecimal("100.00"));
            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    customerId,
                    new BigDecimal("100.00"),
                    LoanStatus.ACTIVE,
                    Instant.now(),
                    new PaymentPlanResponse(BigDecimal.ZERO, List.of())
            );

            when(loanService.createLoan(any(LoanRequest.class))).thenReturn(loanResponse);

            mockMvc.perform(post("/v1/loans")
                            .header("X-Auth-Token", getValidXAuthToken(customerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/v1/loans/" + loanId))
                    .andExpect(jsonPath("$.id", is(loanId.toString())));

            verify(loanService).createLoan(request);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when no authentication token is provided")
        void shouldReturnUnauthorizedWhenNoTokenProvided() throws Exception {
            LoanRequest request = new LoanRequest(UUID.randomUUID(), new BigDecimal("100.00"));

            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

            verify(loanService, never()).createLoan(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request with APZ000006 when amount exceeds available credit line")
        void shouldReturnBadRequestWhenCreditExceeded() throws Exception {
            UUID customerId = UUID.randomUUID();
            LoanRequest request = new LoanRequest(customerId, new BigDecimal("999999.00"));

            when(loanService.createLoan(any(LoanRequest.class)))
                    .thenThrow(new InvalidLoanRequestException("Requested amount exceeds available credit line"));

            mockMvc.perform(post("/v1/loans")
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000006")))
                    .andExpect(jsonPath("$.error", is("INVALID_LOAN_REQUEST")))
                    .andExpect(jsonPath("$.message", containsString("exceeds available credit line")));

            verify(loanService).createLoan(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request with APZ000006 when request validation fails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UUID customerId = UUID.randomUUID();
            String invalidJson = """
                    {
                        "customerId": null,
                        "amount": -50.00
                    }
                    """;

            mockMvc.perform(post("/v1/loans")
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000006")))
                    .andExpect(jsonPath("$.error", is("INVALID_LOAN_REQUEST")));

            verify(loanService, never()).createLoan(any());
        }
    }

    @Nested
    @DisplayName("GET /v1/loans/{loanId} Tests")
    class GetLoanByIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with LoanResponse when loan exists and authenticated")
        void shouldReturnLoanWhenFound() throws Exception {
            UUID loanId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Instant now = Instant.parse("2026-09-18T10:00:00Z");

            LoanResponse loanResponse = new LoanResponse(
                    loanId,
                    customerId,
                    new BigDecimal("400.80"),
                    LoanStatus.ACTIVE,
                    now,
                    new PaymentPlanResponse(new BigDecimal("64.12"), List.of())
            );

            when(loanService.getLoanByExternalId(loanId)).thenReturn(loanResponse);

            mockMvc.perform(get("/v1/loans/{loanId}", loanId)
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(loanId.toString())))
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.amount", is(400.80)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.createdAt", is(now.toString())));

            verify(loanService).getLoanByExternalId(loanId);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when requesting loan without token")
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            UUID loanId = UUID.randomUUID();

            mockMvc.perform(get("/v1/loans/{loanId}", loanId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

            verify(loanService, never()).getLoanByExternalId(any());
        }

        @Test
        @DisplayName("Should return 404 Not Found with APZ000008 when loan is not found")
        void shouldReturnNotFoundWhenLoanMissing() throws Exception {
            UUID loanId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            when(loanService.getLoanByExternalId(loanId)).thenThrow(new LoanNotFoundException(loanId));

            mockMvc.perform(get("/v1/loans/{loanId}", loanId)
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("APZ000008")))
                    .andExpect(jsonPath("$.error", is("LOAN_NOT_FOUND")))
                    .andExpect(jsonPath("$.message", containsString(loanId.toString())));

            verify(loanService).getLoanByExternalId(loanId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when path variable is not a valid UUID")
        void shouldReturnBadRequestForInvalidUUID() throws Exception {
            UUID customerId = UUID.randomUUID();

            mockMvc.perform(get("/v1/loans/{loanId}", "not-a-valid-uuid")
                            .header(HttpHeaders.AUTHORIZATION, getValidBearerToken(customerId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("APZ000004")))
                    .andExpect(jsonPath("$.error", is("INVALID_REQUEST")));

            verify(loanService, never()).getLoanByExternalId(any());
        }
    }
}
