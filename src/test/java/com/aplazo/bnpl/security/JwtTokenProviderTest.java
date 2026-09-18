package com.aplazo.bnpl.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class GenerationTests {

        @Test
        @DisplayName("Should generate a valid signed JWT containing customerId subject and ROLE_CUSTOMER claim")
        void shouldGenerateValidToken() {
            UUID customerId = UUID.randomUUID();

            String token = tokenProvider.generateToken(customerId);

            assertThat(token).isNotBlank();
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);

            // Validate claims directly with parser
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(TEST_SECRET)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertThat(claims.getSubject()).isEqualTo(customerId.toString());
            assertThat(claims.get("role", String.class)).isEqualTo("ROLE_CUSTOMER");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }
    }

    @Nested
    @DisplayName("Customer ID Extraction Tests")
    class SubjectExtractionTests {

        @Test
        @DisplayName("Should extract customer UUID from valid token")
        void shouldExtractCustomerId() {
            UUID customerId = UUID.randomUUID();
            String token = tokenProvider.generateToken(customerId);

            UUID extracted = tokenProvider.getCustomerIdFromToken(token);

            assertThat(extracted).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Should throw exception when extracting customer ID from invalid token")
        void shouldThrowOnInvalidToken() {
            assertThatThrownBy(() -> tokenProvider.getCustomerIdFromToken("invalid.token.string"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldValidateValidToken() {
            UUID customerId = UUID.randomUUID();
            String token = tokenProvider.generateToken(customerId);

            boolean isValid = tokenProvider.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for null, empty, or blank token")
        void shouldReturnFalseForEmptyToken() {
            assertThat(tokenProvider.validateToken(null)).isFalse();
            assertThat(tokenProvider.validateToken("")).isFalse();
            assertThat(tokenProvider.validateToken("   ")).isFalse();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertThat(tokenProvider.validateToken("not.a.valid.jwt.token")).isFalse();
        }

        @Test
        @DisplayName("Should return false for token signed with different key")
        void shouldReturnFalseForTokenWithDifferentKey() {
            String otherSecret = "8x/A?D(G+KbPeShVmYq3t6w9z$C&F)H@McQfTjWnZr4u7x!A%D*G-KaNdRgUkXp2";
            JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, EXPIRATION_MS);

            String token = otherProvider.generateToken(UUID.randomUUID());

            assertThat(tokenProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000L);
            String token = expiredProvider.generateToken(UUID.randomUUID());

            boolean isValid = tokenProvider.validateToken(token);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Authentication Creation Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should produce Authentication object with customerId as principal and ROLE_CUSTOMER authority")
        void shouldProduceValidAuthentication() {
            UUID customerId = UUID.randomUUID();
            String token = tokenProvider.generateToken(customerId);

            Authentication auth = tokenProvider.getAuthentication(token);

            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getPrincipal()).isEqualTo(customerId);
            assertThat(auth.getName()).isEqualTo(customerId.toString());
            assertThat(auth.getCredentials()).isEqualTo(token);
            assertThat(auth.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_CUSTOMER");
        }
    }
}
