package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.RefreshToken;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.RefreshTokenRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.exception.SessionExpiredException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenService jwtTokenService;

    @Value("${clemen.jwt.refresh-token-expiration-hours:12}")
    private long refreshTokenExpirationHours;

    @Override
    @Transactional
    public String createRefreshToken(Usuario usuario) {
        String token = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hashToken(token))
                .expiresAt(LocalDateTime.now().plusHours(refreshTokenExpirationHours))
                .build());
        return token;
    }

    @Override
    @Transactional
    public RefreshTokenRotationResult rotate(String rawRefreshToken) {
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new SessionExpiredException("Refresh token inválido o expirado"));

        if (currentToken.getRevokedAt() != null || currentToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SessionExpiredException("Refresh token inválido o expirado");
        }

        Usuario usuario = usuarioRepository.findById(currentToken.getUsuario().getId())
                .orElseThrow(() -> new SessionExpiredException("Usuario no disponible para refrescar sesión"));

        if (!usuario.isActivo() || usuario.isBloqueado()) {
            throw new SessionExpiredException("Usuario inactivo o bloqueado");
        }

        LocalDateTime now = LocalDateTime.now();
        currentToken.setLastUsedAt(now);
        currentToken.setRevokedAt(now);

        String nextRefreshToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hashToken(nextRefreshToken))
                .expiresAt(now.plusHours(refreshTokenExpirationHours))
                .lastUsedAt(now)
                .build());

        String accessToken = jwtTokenService.generarToken(usuario);
        String expiresAt = jwtTokenService.getAccessTokenExpiresAt().atOffset(ZoneOffset.UTC).toString();

        return new RefreshTokenRotationResult(accessToken, nextRefreshToken, expiresAt);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular hash del refresh token", e);
        }
    }
}
