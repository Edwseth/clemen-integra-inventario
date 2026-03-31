package com.willyes.clemenintegra.inventario.dto.valorizacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AjusteValorizacionLoteResponseDTO(
        Long ajusteId,
        Long loteId,
        String codigoLote,
        Long productoId,
        BigDecimal costoUnitarioAnterior,
        BigDecimal costoUnitarioNuevo,
        BigDecimal costoTotalAnterior,
        BigDecimal costoTotalNuevo,
        BigDecimal totalIngresadoAnterior,
        BigDecimal totalIngresadoNuevo,
        LocalDateTime fechaAjuste,
        Long usuarioId,
        String idempotencyKey
) {
}
