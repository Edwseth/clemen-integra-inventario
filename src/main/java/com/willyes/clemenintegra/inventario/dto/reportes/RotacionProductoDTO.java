package com.willyes.clemenintegra.inventario.dto.reportes;

public record RotacionProductoDTO(
        String producto,
        String sku,
        long cantidadMovimientos,
        String tipoProducto,
        String unidadMedida
) {
}
