package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoOptionDTO {
    private Long id;
    private String nombre;
    private String nombrePlural;
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;
    private UnidadMiniDTO unidad;
    // Texto listo para pintar en el autocomplete (NOMBRE (SKU))
    public String getEtiqueta() {
        return (sku == null || sku.isBlank())
                ? (nombre == null ? "" : nombre)
                : (nombre == null ? sku : nombre + " (" + sku + ")");
    }

    @JsonProperty("codigoSku")
    public String getCodigoSku() {return sku;}

    /* ====== NUEVO: clase anidada para unidad ====== */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UnidadMiniDTO {
        private String nombre;            // p.ej. MILILITRO
        private String nombrePlural;
        private String simbolo;            // ej. KG, ML, M...
        private String simboloImpresion;   // ej. kg, mL, m...
    }
}
