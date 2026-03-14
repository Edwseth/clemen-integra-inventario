package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record LotePendienteUbicarResponseDTO(
        Long loteId,
        String codigoLote,
        Long productoId,
        String nombreProducto,
        String tipoProducto,
        String nombreAlmacen,
        BigDecimal stockDisponible
) {
}
