package com.aplazo.bnpl.security;

import com.aplazo.bnpl.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntryPointTest {

    private CustomAuthenticationEntryPoint entryPoint;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("Should write 401 response with APZ000007 ErrorResponse body")
    void shouldWrite401UnauthorizedErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/loans/12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException authException = new BadCredentialsException("Bad credentials");

        long beforeSeconds = Instant.now().getEpochSecond();
        entryPoint.commence(request, response, authException);
        long afterSeconds = Instant.now().getEpochSecond();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsByteArray(), ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("APZ000007");
        assertThat(errorResponse.error()).isEqualTo("UNAUTHORIZED");
        assertThat(errorResponse.message()).isEqualTo("Unauthorized: Full authentication is required to access this resource");
        assertThat(errorResponse.path()).isEqualTo("/v1/loans/12345");
        assertThat(errorResponse.timestamp()).isBetween(beforeSeconds, afterSeconds);
    }
}
