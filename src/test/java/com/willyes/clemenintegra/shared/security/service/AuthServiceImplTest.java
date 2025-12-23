package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.notification.EmailService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("tester")
                .clave("encoded-pass")
                .nombreCompleto("Tester QA")
                .correo("tester@qa.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .codigo2FA("123456")
                .codigo2FAExpiraEn(LocalDateTime.now().plusMinutes(2))
                .sessionVersion(3L)
                .build();
    }

    @Test
    void verificarCodigo2FAIncrementaVersionYActualizaUltimaActividad() {
        Codigo2FARequestDTO request = new Codigo2FARequestDTO("tester", "123456");

        when(usuarioRepository.findByNombreUsuario("tester")).thenReturn(Optional.of(usuario));
        when(jwtTokenService.generarToken(any(Usuario.class))).thenReturn("jwt-token");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponseDTO response = authService.verificarCodigo2FA(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());

        Usuario actualizado = usuarioCaptor.getValue();
        assertEquals(4L, actualizado.getSessionVersion());
        assertNotNull(actualizado.getUltimaActividad());
        assertNull(actualizado.getCodigo2FA());
        assertNull(actualizado.getCodigo2FAExpiraEn());
        verify(jwtTokenService).generarToken(actualizado);
    }
}
