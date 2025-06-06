package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConEstadoLoteDTO {
    private Long productoId;
    private String codigoSku;
    private String nombre;
    private String estadoLote;
}

