package com.willyes.clemenintegra.inventario.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioReporteDTO(
        LocalDateTime fecha,
        String tipoMovimiento,
        String docReferencia,
        String productoNombre,
        String productoSku,
        BigDecimal cantidad,
        BigDecimal costoUnitarioAplicado,
        BigDecimal costoTotalAplicado,
        String nombreLote,
        String nombreAlmacen,
        String usuarioRegistro
) {
}
