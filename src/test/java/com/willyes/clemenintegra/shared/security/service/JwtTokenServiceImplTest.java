package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
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
}
