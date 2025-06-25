package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponseDTO(
        Long id,
        BigDecimal cantidad,
        String tipoMovimiento,
        String nombreProducto,
        String nombreLote,
        String nombreAlmacen,
        String tipoAlmacen,
        LocalDateTime fechaIngreso,
        String nombreUsuario,
        String nombreMotivo
) { }

