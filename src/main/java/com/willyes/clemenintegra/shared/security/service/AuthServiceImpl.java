package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.notification.EmailService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;

    @Transactional
    public void iniciarLogin(LoginRequestDTO dto) {
        final String username = dto.nombreUsuario() != null ? dto.nombreUsuario().trim() : "";
        final String rawPassword = dto.clave() != null ? dto.clave() : "";

        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!usuario.isActivo() || usuario.isBloqueado()) {
            throw new IllegalStateException("Usuario inactivo o bloqueado");
        }

        if (!passwordEncoder.matches(rawPassword, usuario.getClave())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        // Generar código 2FA de 6 dígitos
        String codigo = String.format("%06d", RNG.nextInt(1_000_000));
        usuario.setCodigo2FA(codigo);
        usuario.setCodigo2FAExpiraEn(LocalDateTime.now().plusMinutes(5));
        usuarioRepository.save(usuario);

        String correoDestino = usuario.getCorreo();
        if (correoDestino == null || correoDestino.isBlank()) {
            throw new IllegalStateException("El usuario no tiene un correo electrónico registrado para el 2FA");
        }

        String asunto = "Código de verificación de Clemen-Integra";
        String cuerpo = String.format("Hola %s,%n%nTu código de verificación es: %s.%n" +
                        "Este código caduca en 5 minutos. Si no solicitaste este acceso, ignora este mensaje.%n%n" +
                        "Equipo Clemen-Integra", usuario.getNombreUsuario(), codigo);

        try {
            emailService.enviarCorreoTexto(correoDestino, asunto, cuerpo);
            log.info("🔐 Código 2FA enviado a {}", correoDestino);
        } catch (MailException ex) {
            log.error("Error al enviar el código 2FA al correo {}", correoDestino, ex);
            throw new IllegalStateException("No se pudo enviar el código de verificación. Intente nuevamente más tarde.");
        }
    }

    @Transactional
    public AuthResponseDTO verificarCodigo2FA(Codigo2FARequestDTO dto) {
        final String username = dto.nombreUsuario() != null ? dto.nombreUsuario().trim() : "";
        final String codigo = dto.codigo() != null ? dto.codigo().trim() : "";

        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o expirado"));

        if (usuario.getCodigo2FA() == null || usuario.getCodigo2FAExpiraEn() == null ||
                !codigo.equals(usuario.getCodigo2FA()) ||
                usuario.getCodigo2FAExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código inválido o expirado");
        }

        // Limpiar código 2FA
        usuario.setCodigo2FA(null);
        usuario.setCodigo2FAExpiraEn(null);
        usuarioRepository.save(usuario);

        String token = jwtTokenService.generarToken(usuario);
        return new AuthResponseDTO(token, usuario.getNombreUsuario(), usuario.getRol().name());
    }
}

