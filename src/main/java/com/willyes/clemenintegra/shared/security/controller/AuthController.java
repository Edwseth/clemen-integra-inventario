package com.willyes.clemenintegra.shared.security.controller;

import com.willyes.clemenintegra.shared.dto.auth.AuthMeResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.RefreshResponseDTO;
import com.willyes.clemenintegra.shared.security.exception.SessionExpiredException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.AuthService;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @Value("${clemen.jwt.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${clemen.jwt.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO dto) {
        authService.iniciarLogin(dto);
        return ResponseEntity.ok("Código de verificación enviado");
    }

    @PostMapping("/verificar")
    public ResponseEntity<AuthResponseDTO> verificarCodigo(@RequestBody Codigo2FARequestDTO dto,
                                                           HttpServletResponse response) {
        AuthResponseDTO authResponse = authService.verificarCodigo2FA(dto);
        String refreshToken = authService.emitirRefreshToken(dto.nombreUsuario());
        addRefreshCookie(response, refreshToken);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SessionExpiredException("Refresh token ausente");
        }

        AuthService.RefreshResult refreshResult = authService.refreshAccessToken(refreshToken);
        addRefreshCookie(response, refreshResult.refreshToken());

        return ResponseEntity.ok(refreshResult.response());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponseDTO> me(Authentication authentication) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        List<String> permisos = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("ROL_"))
                .sorted()
                .toList();

        AuthMeResponseDTO response = new AuthMeResponseDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getRol() != null ? usuario.getRol().name() : null,
                permisos
        );
        return ResponseEntity.ok(response);
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 12)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
