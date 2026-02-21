package com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record VariacionOPResponseDTO(
        Long regularizacionId,
        Long ordenProduccionId,
        BigDecimal cantidadProgramada,
        BigDecimal cantidadReal,
        BigDecimal diferencia,
        BigDecimal rendimientoPct,
        BigDecimal diferenciaPct,
        Boolean ajustarPt,
        String documentoReferencia,
        String observaciones,
        Long usuarioId,
        LocalDateTime fechaIngreso,
        Boolean tieneDetalle,
        Boolean dataInconsistente
) {
}
