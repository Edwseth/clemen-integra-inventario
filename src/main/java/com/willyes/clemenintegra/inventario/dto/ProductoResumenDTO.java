package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO ligero para autocompletados/listados de búsqueda de productos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResumenDTO {
    private Long id;
    private String nombre;
    private String codigoSku;
    private TipoCategoria tipoCategoria;
    private String unidadMedida;
}
