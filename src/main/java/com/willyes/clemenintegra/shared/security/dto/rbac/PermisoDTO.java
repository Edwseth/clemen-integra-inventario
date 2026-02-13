package com.willyes.clemenintegra.shared.security.dto.rbac;

public record PermisoDTO(Long id, String codigo, String modulo, String accion, String descripcion, boolean activo) {
}
