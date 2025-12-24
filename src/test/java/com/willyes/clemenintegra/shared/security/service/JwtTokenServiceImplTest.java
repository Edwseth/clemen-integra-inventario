package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenServiceImplTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void incluyeYRecuperaSessionVersionEnToken() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET);
        Usuario usuario = Usuario.builder()
                .id(99L)
                .nombreUsuario("tester")
                .clave("pass")
                .nombreCompleto("Tester")
                .correo("t@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(5L)
                .build();

        String token = service.generarToken(usuario);

        Long version = service.getSessionVersion(token);
        assertNotNull(version);
        assertEquals(5L, version);

        var claims = service.extraerClaims(token);
        assertEquals("tester", claims.getSubject());
        assertEquals(99L, ((Number) claims.get("usuarioId")).longValue());
    }

    @Test
    void devuelveCeroCuandoTokenNoTieneSessionVersion() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET);
        String token = Jwts.builder()
                .setSubject("legacy")
                .setIssuedAt(new java.util.Date())
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        Long version = service.getSessionVersion(token);
        assertEquals(0L, version);
    }
}
