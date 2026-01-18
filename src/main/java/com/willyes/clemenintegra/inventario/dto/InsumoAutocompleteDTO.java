package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsumoAutocompleteDTO {
    private Integer id;
    private String sku;
    private String nombre;
    private String unidad;
    private Long unidadMedidaId;
    private String unidadMedidaNombre;
}
