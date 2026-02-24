package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final Key secretKey;
    private final long accessTokenExpirationMinutes;

    public JwtTokenServiceImpl(@Value("${clemen.jwt.secret}") String secret,
                               @Value("${clemen.jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public String generarToken(Usuario usuario) {
        Map<String, Object> claims = Map.of(
                "rol", usuario.getRol().name(),
                "usuarioId", usuario.getId(),
                "sessionVersion", usuario.getSessionVersion()
        );
        return Jwts.builder()
                .setSubject(usuario.getNombreUsuario())
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(getAccessTokenExpiresAt()))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public Instant getAccessTokenExpiresAt() {
        return Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extraerNombreUsuario(String token) {
        return extraerClaims(token).getSubject();
    }

    public Long getSessionVersion(String token) {
        return getSessionVersion(extraerClaims(token));
    }

    @Override
    public Long getSessionVersion(Claims claims) {
        Object value = claims.get("sessionVersion");
        if (value == null) {
            return 0L;
        }

        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("sessionVersion inválida en token");
            }
        }
        throw new IllegalArgumentException("sessionVersion inválida en token");
    }


}
