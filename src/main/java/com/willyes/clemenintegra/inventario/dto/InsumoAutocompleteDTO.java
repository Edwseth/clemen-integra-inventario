package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsumoAutocompleteDTO {
    private Long id;
    private String sku;
    private String nombre;
    private String unidad;
}
