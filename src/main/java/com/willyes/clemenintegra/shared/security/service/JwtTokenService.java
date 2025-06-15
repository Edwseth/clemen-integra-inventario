package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import io.jsonwebtoken.Claims;

public interface JwtTokenService {
    String generarToken(Usuario usuario);
    Claims extraerClaims(String token);
    String extraerNombreUsuario(String token);
}
