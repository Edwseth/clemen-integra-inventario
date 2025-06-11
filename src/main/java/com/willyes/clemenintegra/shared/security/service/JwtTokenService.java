package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtTokenService {

    private static final long EXPIRATION_MS = 3600_000; // 1 hora
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor("clave-super-secreta-de-al-menos-32-caracteres".getBytes());

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getNombreUsuario())
                .claim("rol", usuario.getRol().name())
                .claim("usuarioId", usuario.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extraerNombreUsuario(String token) {
        return extraerClaims(token).getSubject();
    }


}

