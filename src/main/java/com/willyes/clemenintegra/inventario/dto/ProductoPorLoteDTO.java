package com.willyes.clemenintegra.inventario.dto;

/**
 * Información mínima del producto asociado a un lote.
 */
public record ProductoPorLoteDTO(
        Long productoId,
        String sku,
        String nombreProducto
) {
}
