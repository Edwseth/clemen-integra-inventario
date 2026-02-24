package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import io.jsonwebtoken.Claims;

import java.time.Instant;

public interface JwtTokenService {
    String generarToken(Usuario usuario);
    Instant getAccessTokenExpiresAt();
    Claims extraerClaims(String token);
    String extraerNombreUsuario(String token);
    Long getSessionVersion(String token);
    Long getSessionVersion(Claims claims);
}
