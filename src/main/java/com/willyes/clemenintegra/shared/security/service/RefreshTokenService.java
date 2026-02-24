package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;

public interface RefreshTokenService {

    String createRefreshToken(Usuario usuario);

    RefreshTokenRotationResult rotate(String rawRefreshToken);

    record RefreshTokenRotationResult(String accessToken, String refreshToken, String expiresAt) {
    }
}
