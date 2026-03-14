package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LotePendienteUbicarResponseDTO(
        Long loteId,
        String codigoLote,
        Long productoId,
        String nombreProducto,
        String tipoProducto,
        BigDecimal stockDisponible,
        LocalDateTime fechaVencimiento,
        String estado,
        Long almacenIdActual,
        String nombreAlmacenActual,
        Long almacenDestinoSugeridoId,
        String nombreAlmacenDestinoSugerido,
        boolean requiereUbicacionDestino
) {
}
