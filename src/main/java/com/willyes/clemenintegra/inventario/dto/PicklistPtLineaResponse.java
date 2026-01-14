package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;

import java.math.BigDecimal;

public record PicklistPtLineaResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        BigDecimal cantidad,
        PicklistPtModoAsignacion modoAsignacion,
        Long loteProductoId,
        String codigoLote
) {
}
