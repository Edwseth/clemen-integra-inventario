package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public record InventarioGeneralPreviewRowDTO(
        String sku,
        String nombre,
        String udm,
        BigDecimal cant,
        String lote,
        String vence,
        String ubicacion
) {
}
