package com.willyes.clemenintegra.inventario.dto.valorizacion;

import java.math.BigDecimal;
import java.util.List;

public record ValorizacionElegibilidadResponseDTO(
        boolean eligible,
        Snapshot snapshot,
        List<ReglaEvaluada> reglas,
        List<String> warnings
) {

    public record Snapshot(
            Long loteId,
            String codigoLote,
            Long productoId,
            String nombreProducto,
            Integer almacenId,
            String nombreAlmacen,
            String estado,
            BigDecimal stockDisponible,
            BigDecimal costoUnitarioActual,
            BigDecimal costoTotalActual,
            BigDecimal totalIngresadoActual
    ) {
    }

    public record ReglaEvaluada(
            String code,
            boolean passed,
            String severity,
            String message
    ) {
    }
}
