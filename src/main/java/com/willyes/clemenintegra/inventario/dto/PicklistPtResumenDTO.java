package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;

import java.time.LocalDateTime;

public record PicklistPtResumenDTO(
        Long id,
        String codigo,
        String clienteNombre,
        PicklistPtEstado estado,
        LocalDateTime fechaCreacion
) {
}
