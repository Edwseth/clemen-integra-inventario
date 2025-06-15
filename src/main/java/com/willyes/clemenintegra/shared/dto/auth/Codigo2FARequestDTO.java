package com.willyes.clemenintegra.shared.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record Codigo2FARequestDTO(
        @NotBlank String nombreUsuario,
        @NotBlank String codigo
) { }

