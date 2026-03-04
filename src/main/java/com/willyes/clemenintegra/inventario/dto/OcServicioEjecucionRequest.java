package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OcServicioEjecucionRequest(
        Long detalleId,
        BigDecimal cantidadEjecutada,
        LocalDateTime fechaEjecucion,
        String observaciones
) {
}
