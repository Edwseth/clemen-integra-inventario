package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PicklistPtAsignacionResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        Long loteProductoId,
        String codigoLote,
        BigDecimal cantidadAsignada,
        LocalDateTime fechaVencimiento,
        Integer almacenId,
        Short orden
) {
}
