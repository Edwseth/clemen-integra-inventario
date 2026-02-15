package com.willyes.clemenintegra.shared.security.dto.rbac;

public record PermisoDTO(Long id, String codigo, String descripcion, String modulo, boolean activo, String tipo) {
}
