package com.willyes.clemenintegra.inventario.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventarioValorizadoRowDTO(
        String sku,
        String nombre,
        String udm,
        String lote,
        LocalDate vence,
        String ubicacionAlmacen,
        BigDecimal stock,
        BigDecimal costoUnitarioMaterial,
        BigDecimal valorTotal
) {
}
