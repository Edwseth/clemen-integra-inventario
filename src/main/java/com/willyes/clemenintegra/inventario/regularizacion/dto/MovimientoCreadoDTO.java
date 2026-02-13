package com.willyes.clemenintegra.inventario.regularizacion.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MovimientoCreadoDTO(
        Long movimientoId,
        String tipoMovimiento,
        String clasificacion,
        Long loteProductoId,
        BigDecimal cantidad
) {
}
