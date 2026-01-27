package com.willyes.clemenintegra.shared.dto.auth;

import java.util.List;

public record AuthMeResponseDTO(Long usuarioId, String username, String nombre, String rol, List<String> permisos) {
}
