package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public record MovimientoInventarioResponseDTO(
        Long id,
        BigDecimal cantidad,
        Long productoId,
        String tipoMovimiento,
        String nombreProducto,
        String nombreLote,
        String nombreAlmacen
        // agrega otros campos según tu necesidad
) { }

