package com.willyes.clemenintegra.shared.security.controller;

import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;
import com.willyes.clemenintegra.shared.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO dto) {
        authService.iniciarLogin(dto);
        return ResponseEntity.ok("Código de verificación enviado");
    }

    @PostMapping("/verificar")
    public ResponseEntity<AuthResponseDTO> verificarCodigo(@RequestBody Codigo2FARequestDTO dto) {
        return ResponseEntity.ok(authService.verificarCodigo2FA(dto));
    }
}

