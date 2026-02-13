package com.willyes.clemenintegra.inventario.regularizacion.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RegularizacionTrazabilidadResponseDTO(
        Long operacionId,
        String idempotencyKey,
        Long ordenProduccionId,
        Long productoId,
        String tipoOperacion,
        List<MovimientoCreadoDTO> movimientos,
        Long registradoPorId,
        LocalDateTime fecha
) {
}
