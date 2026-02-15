package com.willyes.clemenintegra.shared.security.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateRolPermisosRequest(@NotNull List<Long> permisoIds, @NotBlank String modulo) {
}
