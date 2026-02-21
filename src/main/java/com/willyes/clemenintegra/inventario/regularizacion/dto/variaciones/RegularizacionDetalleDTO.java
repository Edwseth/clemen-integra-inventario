package com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones;

import java.math.BigDecimal;

public record RegularizacionDetalleDTO(
        Long productoId,
        Long loteId,
        BigDecimal cantidad,
        String tipo,
        Integer almacenOrigenId,
        Integer almacenDestinoId,
        Long movimientoId
) {
}
