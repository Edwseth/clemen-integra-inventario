package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public void iniciarLogin(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(dto.nombreUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!usuario.isActivo() || usuario.isBloqueado()) {
            throw new IllegalStateException("Usuario inactivo o bloqueado");
        }

        if (!passwordEncoder.matches(dto.clave(), usuario.getClave())) {
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        // Generar código 2FA
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));
        usuario.setCodigo2FA(codigo);
        usuario.setCodigo2FAExpiraEn(LocalDateTime.now().plusMinutes(5));

        usuarioRepository.save(usuario);

        // Aquí podrías enviar el código por email o WhatsApp si se desea
        log.info("\uD83D\uDD10 Código 2FA generado: {}", codigo);
    }

    @Transactional
    public AuthResponseDTO verificarCodigo2FA(Codigo2FARequestDTO dto) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(dto.nombreUsuario())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        log.info("Validando código 2FA...");
        log.info("Usuario: {}", usuario.getNombreUsuario());
        log.info("Codigo 2FA almacenado: {}", usuario.getCodigo2FA());
        log.info("Codigo 2FA recibido: {}", dto.codigo());
        log.info("Fecha expiración: {}", usuario.getCodigo2FAExpiraEn());
        log.info("Fecha actual: {}", LocalDateTime.now());
        if (usuario.getCodigo2FA() == null || usuario.getCodigo2FAExpiraEn() == null ||
                !usuario.getCodigo2FA().equals(dto.codigo()) ||
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
