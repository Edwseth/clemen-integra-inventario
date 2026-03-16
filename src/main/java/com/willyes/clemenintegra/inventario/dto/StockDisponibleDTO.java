package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockDisponibleDTO(
        String sku,
        String nombre,
        String categoria,
        BigDecimal cantidadActual,
        String lote,
        LocalDate fechaVencimiento,
        String ubicacion
) {
}
