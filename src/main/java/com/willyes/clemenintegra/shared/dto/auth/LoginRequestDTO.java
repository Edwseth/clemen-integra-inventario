package com.willyes.clemenintegra.shared.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String nombreUsuario,
        @NotBlank String clave
) { }

