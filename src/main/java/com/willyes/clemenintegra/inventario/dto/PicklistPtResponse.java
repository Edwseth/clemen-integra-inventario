package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;

import java.time.LocalDateTime;
import java.util.List;

public record PicklistPtResponse(
        Long id,
        String codigo,
        String clienteNombre,
        Integer minVidaUtilDias,
        PicklistPtEstado estado,
        Integer almacenPtId,
        Integer tipoMovimientoDetalleId,
        String docReferencia,
        String observaciones,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaConfirmacion,
        LocalDateTime fechaEjecucion,
        List<PicklistPtLineaResponse> lineas,
        List<PicklistPtAsignacionResponse> asignaciones
) {
}
