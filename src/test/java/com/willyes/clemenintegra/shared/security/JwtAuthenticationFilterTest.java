package com.willyes.clemenintegra.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.shared.security.exception.SesionExpiradaAuthenticationException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaAuthenticationException;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private JwtAuthenticationFilter filter;
    private AuthenticationEntryPoint authenticationEntryPoint;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        authenticationManagerProvider = mock(ObjectProvider.class);
        when(authenticationManagerProvider.getIfAvailable()).thenReturn(authenticationManager);
        authenticationEntryPoint = new ApiAuthenticationEntryPoint(new ObjectMapper());
        filter = new JwtAuthenticationFilter(authenticationManagerProvider, authenticationEntryPoint);
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
    void responde401CuandoSesionInvalidada() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new SesionInvalidadaAuthenticationException("Sesión invalidada"));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }

    @Test
    void responde401CuandoSesionInactiva() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new SesionExpiradaAuthenticationException("Inactiva"));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }

    @Test
    void responde401ParaErroresDeAutenticacion() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any(JwtAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(authenticationManager, times(1)).authenticate(any(JwtAuthenticationToken.class));
    }

    @Test
    void omiteValidacionCuandoNoHayAuthenticationManager() throws ServletException, IOException {
        when(authenticationManagerProvider.getIfAvailable()).thenReturn(null);
        JwtAuthenticationFilter filterSinManager = new JwtAuthenticationFilter(authenticationManagerProvider, authenticationEntryPoint);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filterSinManager.doFilterInternal(request, response, new MockFilterChain());

        verify(authenticationManager, never()).authenticate(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
