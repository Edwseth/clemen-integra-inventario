package com.willyes.clemenintegra.shared.security.controller;

import com.willyes.clemenintegra.shared.dto.auth.AuthMeResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.AuthService;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import java.util.List;
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

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO dto) {
        authService.iniciarLogin(dto);
        return ResponseEntity.ok("Código de verificación enviado");
    }

    @PostMapping("/verificar")
    public ResponseEntity<AuthResponseDTO> verificarCodigo(@RequestBody Codigo2FARequestDTO dto) {
        return ResponseEntity.ok(authService.verificarCodigo2FA(dto));
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
}
