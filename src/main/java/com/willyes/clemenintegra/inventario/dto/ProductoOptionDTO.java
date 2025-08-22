package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoOptionDTO {
    private Long id;
    private String nombre;
    private String codigoSku;
    // Texto listo para pintar en el autocomplete (NOMBRE (SKU))
    public String getEtiqueta() {
        return (codigoSku == null || codigoSku.isBlank())
                ? (nombre == null ? "" : nombre)
                : (nombre == null ? codigoSku : nombre + " (" + codigoSku + ")");
    }
}
