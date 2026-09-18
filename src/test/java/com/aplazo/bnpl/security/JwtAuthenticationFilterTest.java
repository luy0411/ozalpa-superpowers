package com.aplazo.bnpl.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate when valid Bearer token is provided in Authorization header")
    void shouldAuthenticateWithBearerAuthorizationHeader() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID customerId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customerId,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/loans");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(contextAuth).isNotNull();
        assertThat(contextAuth.getPrincipal()).isEqualTo(customerId);
        assertThat(contextAuth.getDetails()).isInstanceOf(WebAuthenticationDetails.class);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate when valid token is provided in X-Auth-Token header")
    void shouldAuthenticateWithXAuthTokenHeader() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID customerId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customerId,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/loans");
        request.addHeader("X-Auth-Token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(contextAuth).isNotNull();
        assertThat(contextAuth.getPrincipal()).isEqualTo(customerId);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate when valid Bearer token is provided in X-Auth-Token header")
    void shouldAuthenticateWithBearerPrefixInXAuthTokenHeader() throws ServletException, IOException {
        String token = "valid.jwt.token";
        UUID customerId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customerId,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/loans");
        request.addHeader("X-Auth-Token", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(contextAuth).isNotNull();
        assertThat(contextAuth.getPrincipal()).isEqualTo(customerId);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when token is invalid")
    void shouldNotAuthenticateWhenTokenInvalid() throws ServletException, IOException {
        String token = "invalid.token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/loans");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).getAuthentication(any());
    }

    @Test
    @DisplayName("Should not authenticate when no token headers are present")
    void shouldNotAuthenticateWhenNoHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(any());
        verify(jwtTokenProvider, never()).getAuthentication(any());
    }
}
