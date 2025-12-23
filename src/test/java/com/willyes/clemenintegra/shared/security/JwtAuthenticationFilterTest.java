package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        filter = new JwtAuthenticationFilter(authenticationManager);
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegaEnAuthenticationManagerYAsignaContextoCuandoTokenEsValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                "user", "sample-token", List.of(() -> "ROLE_USER")
        );
        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class))).thenReturn(authResult);

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user", authentication.getName());
        assertTrue(authentication.isAuthenticated());

        ArgumentCaptor<JwtAuthenticationToken> captor = ArgumentCaptor.forClass(JwtAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("sample-token", captor.getValue().getCredentials());
    }

    @Test
    void propagaSesionInvalidadaExceptionYNoSeteaContexto() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new SesionInvalidadaException("Sesion invalidada"));

        assertThrows(SesionInvalidadaException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
