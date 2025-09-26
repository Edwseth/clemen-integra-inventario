package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LotePtDisponibleDTO(
        Long id,
        String codigoLote,
        BigDecimal stockDisponible,
        LocalDateTime fechaVencimiento
) {
}
