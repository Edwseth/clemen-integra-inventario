package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConLotesDTO {
    private Long productoId;
    private String codigoSku;
    private String nombre;
    private List<LoteSimpleDTO> lotes;
}
