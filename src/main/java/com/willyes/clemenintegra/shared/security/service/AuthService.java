package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.dto.auth.AuthResponseDTO;
import com.willyes.clemenintegra.shared.dto.auth.Codigo2FARequestDTO;
import com.willyes.clemenintegra.shared.dto.auth.LoginRequestDTO;

public interface AuthService {
    void iniciarLogin(LoginRequestDTO dto);
    AuthResponseDTO verificarCodigo2FA(Codigo2FARequestDTO dto);
}
