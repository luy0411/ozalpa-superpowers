package com.aplazo.bnpl.security;

import com.aplazo.bnpl.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestSecurityController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, JwtTokenProvider.class, GlobalExceptionHandler.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @RestController
    static class TestSecurityController {

        @PostMapping("/v1/customers")
        public String createCustomer() {
            return "customer-created";
        }

        @GetMapping("/v1/customers/00000000-0000-0000-0000-000000000001")
        public String getCustomer() {
            return "customer-found";
        }

        @PostMapping("/v1/loans")
        public String createLoan() {
            return "loan-created";
        }

        @GetMapping("/v1/loans/00000000-0000-0000-0000-000000000001")
        public String getLoan() {
            return "loan-found";
        }

        @GetMapping("/swagger-ui/index.html")
        public String swaggerUi() {
            return "swagger-ui";
        }

        @GetMapping("/swagger-ui.html")
        public String swaggerUiHtml() {
            return "swagger-ui-html";
        }

        @GetMapping("/v3/api-docs")
        public String apiDocs() {
            return "api-docs";
        }

        @GetMapping("/actuator/health")
        public String actuatorHealth() {
            return "healthy";
        }
    }

    @Nested
    @DisplayName("Public Routes")
    class PublicRoutesTests {

        @Test
        @DisplayName("POST /v1/customers should be permitted without authentication")
        void postCustomersShouldBePermitted() throws Exception {
            mockMvc.perform(post("/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/swagger-ui/** should be permitted without authentication")
        void swaggerUiShouldBePermitted() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/swagger-ui.html should be permitted without authentication")
        void swaggerUiHtmlShouldBePermitted() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/v3/api-docs/** should be permitted without authentication")
        void apiDocsShouldBePermitted() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/** should be permitted without authentication")
        void actuatorShouldBePermitted() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Routes - Unauthorized (401)")
    class UnauthorizedTests {

        @Test
        @DisplayName("GET /v1/customers/{id} without token should return 401 with APZ000007 format")
        void getCustomerWithoutTokenShouldReturn401() throws Exception {
            mockMvc.perform(get("/v1/customers/00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.message", is("Unauthorized: Full authentication is required to access this resource")))
                    .andExpect(jsonPath("$.path", is("/v1/customers/00000000-0000-0000-0000-000000000001")));
        }

        @Test
        @DisplayName("POST /v1/loans without token should return 401 with APZ000007 format")
        void postLoansWithoutTokenShouldReturn401() throws Exception {
            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                    .andExpect(jsonPath("$.message", is("Unauthorized: Full authentication is required to access this resource")))
                    .andExpect(jsonPath("$.path", is("/v1/loans")));
        }

        @Test
        @DisplayName("Protected route with invalid Bearer token should return 401 with APZ000007 format")
        void protectedRouteWithInvalidTokenShouldReturn401() throws Exception {
            mockMvc.perform(get("/v1/loans/00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Protected route with invalid X-Auth-Token should return 401 with APZ000007 format")
        void protectedRouteWithInvalidXAuthTokenShouldReturn401() throws Exception {
            mockMvc.perform(get("/v1/loans/00000000-0000-0000-0000-000000000001")
                            .header("X-Auth-Token", "invalid.jwt.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("APZ000007")))
                    .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("Protected Routes - Authorized (200)")
    class AuthorizedTests {

        @Test
        @DisplayName("Protected route with valid Bearer token should succeed")
        void protectedRouteWithBearerTokenShouldSucceed() throws Exception {
            UUID customerId = UUID.randomUUID();
            String token = jwtTokenProvider.generateToken(customerId);

            mockMvc.perform(get("/v1/loans/00000000-0000-0000-0000-000000000001")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Protected route with valid X-Auth-Token should succeed")
        void protectedRouteWithXAuthTokenShouldSucceed() throws Exception {
            UUID customerId = UUID.randomUUID();
            String token = jwtTokenProvider.generateToken(customerId);

            mockMvc.perform(get("/v1/loans/00000000-0000-0000-0000-000000000001")
                            .header("X-Auth-Token", token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /v1/loans with valid token should succeed without CSRF token (CSRF disabled)")
        void postLoansWithValidTokenWithoutCsrfShouldSucceed() throws Exception {
            UUID customerId = UUID.randomUUID();
            String token = jwtTokenProvider.generateToken(customerId);

            mockMvc.perform(post("/v1/loans")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }
}
