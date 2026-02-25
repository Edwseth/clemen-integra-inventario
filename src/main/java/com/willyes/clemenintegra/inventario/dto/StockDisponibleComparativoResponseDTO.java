package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockDisponibleComparativoResponseDTO(
        String sku,
        String nombre,
        String udm,
        BigDecimal cantidadFechaIndicada,
        BigDecimal cantidadActual,
        BigDecimal diferencia,
        String lote,
        LocalDate vence,
        String ubicacion
) {
}
