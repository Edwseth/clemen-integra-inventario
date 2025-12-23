package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {

    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtAuthenticationProvider(jwtTokenService, userDetailsService, usuarioRepository);
        ReflectionTestUtils.setField(provider, "maxIdleMinutes", 20L);
    }

    @Test
    void lanzaSesionInvalidadaCuandoVersionTokenNoCoincide() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(2L)
                .build();

        when(jwtTokenService.extraerClaims("token"))
                .thenReturn(new DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));

        assertThrows(SesionInvalidadaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void lanzaSesionInactivaCuandoSuperaMaximoInactividad() {
        LocalDateTime hace25Min = LocalDateTime.now().minusMinutes(25);
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(1L)
                .ultimaActividad(hace25Min)
                .build();

        when(jwtTokenService.extraerClaims("token"))
                .thenReturn(new DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));

        assertThrows(SesionInactivaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void lanzaSesionInactivaCuandoUltimaActividadEsNull() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(1L)
                .ultimaActividad(null)
                .build();

        when(jwtTokenService.extraerClaims("token"))
                .thenReturn(new DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));

        assertThrows(SesionInactivaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizaUltimaActividadCuandoSesionEsValida() {
        LocalDateTime hace2Min = LocalDateTime.now().minusMinutes(2);
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(1L)
                .ultimaActividad(hace2Min)
                .build();

        when(jwtTokenService.extraerClaims("token"))
                .thenReturn(new DefaultClaims().setSubject("user"));
        when(jwtTokenService.getSessionVersion("token")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(new CustomUserDetails(usuario));
        when(usuarioRepository.save(eq(usuario))).thenReturn(usuario);

        provider.authenticate(new JwtAuthenticationToken("token"));

        verify(usuarioRepository, times(1)).save(eq(usuario));
        verify(jwtTokenService, times(1)).extraerClaims("token");
        verify(jwtTokenService, times(1)).getSessionVersion("token");
        assertNotNull(usuario.getUltimaActividad());
        assertTrue(usuario.getUltimaActividad().isAfter(hace2Min));
    }
}
