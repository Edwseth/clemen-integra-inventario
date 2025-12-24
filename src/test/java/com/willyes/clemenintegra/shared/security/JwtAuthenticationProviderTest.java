package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
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
    private UsuarioRepository usuarioRepository;

    private JwtAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtAuthenticationProvider(jwtTokenService, usuarioRepository);
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

        Claims claims = new DefaultClaims();
        claims.setSubject("user");
        claims.put("sessionVersion", 1L);
        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(1L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));

        assertThrows(SesionInvalidadaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).saveAndFlush(any());
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

        Claims claims = new DefaultClaims();
        claims.setSubject("user");
        claims.put("sessionVersion", 1L);
        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(1L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));

        assertThrows(SesionInactivaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void permitePrimeraActividadCuandoUltimaActividadEsNull() {
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

        Claims claims = new DefaultClaims();
        claims.setSubject("user");
        claims.put("sessionVersion", 1L);
        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(1L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var authentication = provider.authenticate(new JwtAuthenticationToken("token"));

        verify(usuarioRepository, times(1)).saveAndFlush(usuario);
        assertNotNull(usuario.getUltimaActividad());
        assertTrue(authentication.isAuthenticated());
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

        Claims claims = new DefaultClaims();
        claims.setSubject("user");
        claims.put("sessionVersion", 1L);
        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(1L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(eq(usuario))).thenReturn(usuario);

        var authentication = provider.authenticate(new JwtAuthenticationToken("token"));

        verify(usuarioRepository, times(1)).saveAndFlush(eq(usuario));
        verify(jwtTokenService, times(1)).extraerClaims("token");
        verify(jwtTokenService, times(1)).getSessionVersion(claims);
        assertNotNull(usuario.getUltimaActividad());
        assertTrue(usuario.getUltimaActividad().isAfter(hace2Min));
        assertTrue(authentication.isAuthenticated());
        assertEquals("user", authentication.getName());
        assertEquals(1, authentication.getAuthorities().size());
    }

    @Test
    void aceptaVersionPorDefectoCuandoTokenNoTieneSessionVersion() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(0L)
                .ultimaActividad(LocalDateTime.now().minusMinutes(1))
                .build();

        Claims claims = new DefaultClaims();
        claims.setSubject("user");
        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(0L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        Authentication authentication = provider.authenticate(new JwtAuthenticationToken("token"));

        assertTrue(authentication.isAuthenticated());
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void tokenLegadoSeInvalidaCuandoDbTieneVersionMayorACero() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario("user")
                .clave("pass")
                .nombreCompleto("Nombre")
                .correo("correo@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(3L)
                .ultimaActividad(LocalDateTime.now())
                .build();

        Claims claims = new DefaultClaims();
        claims.setSubject("user");

        when(jwtTokenService.extraerClaims("token")).thenReturn(claims);
        when(jwtTokenService.getSessionVersion(claims)).thenReturn(0L);
        when(usuarioRepository.findByNombreUsuario("user")).thenReturn(java.util.Optional.of(usuario));

        assertThrows(SesionInvalidadaException.class,
                () -> provider.authenticate(new JwtAuthenticationToken("token")));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }
}
