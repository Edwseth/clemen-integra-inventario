package com.willyes.clemenintegra.inventario.regularizacion.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RegularizacionTrazabilidadResponseDTO(
        Long regularizacionId,
        String idempotencyKey,
        Long ordenProduccionId,
        BigDecimal cantidadProgramada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        List<MovimientoCreadoDTO> movimientos,
        Long registradoPorId,
        LocalDateTime fecha
) {
}
