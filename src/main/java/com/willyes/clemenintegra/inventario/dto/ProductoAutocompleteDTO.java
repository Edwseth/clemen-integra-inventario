package com.willyes.clemenintegra.inventario.dto;

public record ProductoAutocompleteDTO(
        Integer id,
        String codigoSku,
        String nombre,
        UnidadMedidaAutocompleteDTO unidadMedida
) {
    public ProductoAutocompleteDTO(Integer id, String codigoSku, String nombre) {
        this(id, codigoSku, nombre, null);
    }
}
