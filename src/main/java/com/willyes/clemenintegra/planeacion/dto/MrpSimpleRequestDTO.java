package com.willyes.clemenintegra.planeacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MrpSimpleRequestDTO {
    private LocalDate horizonteDesde;
    private LocalDate horizonteHasta;
    private List<String> categoriasProducto;
    private Boolean soloControlStock;
    private Boolean incluirProductosSinParametros;
}
