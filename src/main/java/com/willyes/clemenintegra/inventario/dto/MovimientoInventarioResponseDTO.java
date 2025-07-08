package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponseDTO(
        Long id,
        BigDecimal cantidad,
        String tipoMovimiento,
        String nombreProducto,
        String nombreLote,
        String nombreAlmacenOrigen,
        String nombreAlmacenDestino,
        String tipoAlmacenOrigen,
        String tipoAlmacenDestino,
        LocalDateTime fechaIngreso,
        String nombreUsuario,
        String nombreMotivo
) { }

