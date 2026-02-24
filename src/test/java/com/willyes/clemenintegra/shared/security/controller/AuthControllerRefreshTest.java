package com.willyes.clemenintegra.shared.security.controller;

import com.willyes.clemenintegra.shared.dto.auth.RefreshResponseDTO;
import com.willyes.clemenintegra.shared.security.exception.SessionExpiredException;
import com.willyes.clemenintegra.shared.security.service.AuthService;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerRefreshTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        UsuarioService usuarioService = Mockito.mock(UsuarioService.class);

        AuthController controller = new AuthController(authService, usuarioService);
        ReflectionTestUtils.setField(controller, "refreshCookieName", "refresh_token");
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", false);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.willyes.clemenintegra.shared.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void refreshValidoDevuelve200YRotaCookie() throws Exception {
        AuthService.RefreshResult result = new AuthService.RefreshResult(
                new RefreshResponseDTO("new-access-token", "2026-01-01T10:00:00Z"),
                "new-refresh-token"
        );
        Mockito.when(authService.refreshAccessToken(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresAt").value("2026-01-01T10:00:00Z"))
                .andExpect(cookie().value("refresh_token", "new-refresh-token"));
    }

    @Test
    void refreshInvalidoDevuelve401SessionExpired() throws Exception {
        Mockito.when(authService.refreshAccessToken(anyString()))
                .thenThrow(new SessionExpiredException("Refresh token inválido o expirado"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }
}
