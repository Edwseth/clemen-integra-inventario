package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

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
    void delegaAutenticacionCuandoTokenEsValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        UsernamePasswordAuthenticationToken authenticated =
                new UsernamePasswordAuthenticationToken("user", "sample-token", java.util.List.of(() -> "ROLE_USER"));
        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class))).thenReturn(authenticated);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user", authentication.getName());
        assertTrue(authentication.isAuthenticated());
        ArgumentCaptor<JwtAuthenticationToken> captor = ArgumentCaptor.forClass(JwtAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(captor.capture());
        assertEquals("sample-token", captor.getValue().getCredentials());
    }

    @Test
    void lanzaSesionInvalidadaCuandoVersionNoCoincide() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new SesionInvalidadaException("Sesión invalidada"));

        assertThrows(SesionInvalidadaException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }

    @Test
    void lanzaSesionInactivaCuandoSuperaTiempoInactividad() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new SesionInactivaException("Inactiva"));

        assertThrows(SesionInactivaException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }

    @Test
    void burbujeaErroresDeAutenticacion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }
}
