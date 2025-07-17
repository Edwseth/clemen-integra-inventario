package com.willyes.clemenintegra.inventario.dto.reportes;

import java.math.BigDecimal;

public record ProductoCostosoDTO(
        String producto,
        String sku,
        BigDecimal precioUnitario,
        String proveedor,
        String tipoProducto
) {
}
