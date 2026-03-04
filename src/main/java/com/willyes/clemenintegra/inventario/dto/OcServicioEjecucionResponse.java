package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OcServicioEjecucionResponse(
        LocalDateTime fecha,
        Long detalleId,
        BigDecimal cantidad,
        String observaciones,
        String usuarioNombre
) {
}
