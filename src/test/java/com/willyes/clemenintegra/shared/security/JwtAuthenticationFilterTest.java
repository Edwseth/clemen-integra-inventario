package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private CustomUserDetailsService userDetailsService;
    private UsuarioRepository usuarioRepository;
    private ObjectProvider<JwtTokenService> jwtTokenServiceProvider;
    private ObjectProvider<CustomUserDetailsService> userDetailsServiceProvider;
    private ObjectProvider<UsuarioRepository> usuarioRepositoryProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        jwtTokenServiceProvider = mock(ObjectProvider.class);
        userDetailsServiceProvider = mock(ObjectProvider.class);
        usuarioRepositoryProvider = mock(ObjectProvider.class);

        when(jwtTokenServiceProvider.getIfAvailable()).thenReturn(jwtTokenService);
        when(userDetailsServiceProvider.getIfAvailable()).thenReturn(userDetailsService);
        when(usuarioRepositoryProvider.getIfAvailable()).thenReturn(usuarioRepository);

        filter = new JwtAuthenticationFilter(jwtTokenServiceProvider, userDetailsServiceProvider, usuarioRepositoryProvider);
        ReflectionTestUtils.setField(filter, "maxIdleMinutes", 20L);
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaYActualizaUltimaActividadCuandoTokenEsValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        Usuario usuario = Usuario.builder()
                .nombreUsuario("user")
                .sessionVersion(1L)
                .rol(com.willyes.clemenintegra.shared.model.enums.RolUsuario.ROL_SUPER_ADMIN)
                .ultimaActividad(java.time.LocalDateTime.now())
                .build();

        when(jwtTokenService.extraerClaims("sample-token"))
                .thenReturn(new io.jsonwebtoken.impl.DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("sample-token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));
        when(usuarioRepository.save(eq(usuario))).thenReturn(usuario);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user", authentication.getName());
        assertTrue(authentication.isAuthenticated());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertNotNull(captor.getValue().getUltimaActividad());
        assertEquals("user", ((CustomUserDetails) authentication.getPrincipal()).getUsername());
    }

    @Test
    void lanzaSesionInvalidadaCuandoVersionNoCoincide() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Usuario usuario = Usuario.builder()
                .nombreUsuario("user")
                .sessionVersion(2L)
                .rol(com.willyes.clemenintegra.shared.model.enums.RolUsuario.ROL_SUPER_ADMIN)
                .ultimaActividad(java.time.LocalDateTime.now())
                .build();

        when(jwtTokenService.extraerClaims("expired-token"))
                .thenReturn(new io.jsonwebtoken.impl.DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("expired-token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));

        assertThrows(SesionInvalidadaException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void lanzaSesionInactivaCuandoSuperaTiempoInactividad() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        java.time.LocalDateTime hace30Min = java.time.LocalDateTime.now().minusMinutes(30);
        Usuario usuario = Usuario.builder()
                .nombreUsuario("user")
                .sessionVersion(1L)
                .rol(com.willyes.clemenintegra.shared.model.enums.RolUsuario.ROL_SUPER_ADMIN)
                .ultimaActividad(hace30Min)
                .build();

        when(jwtTokenService.extraerClaims("inactive-token"))
                .thenReturn(new io.jsonwebtoken.impl.DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("inactive-token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));

        assertThrows(SesionInactivaException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void lanzaBadCredentialsParaTokenInvalido() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenService.extraerClaims("bad-token"))
                .thenThrow(new io.jsonwebtoken.security.SignatureException("bad"));

        assertThrows(BadCredentialsException.class, () ->
                filter.doFilterInternal(request, response, new MockFilterChain())
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(usuarioRepository, never()).save(any());
    }
}
