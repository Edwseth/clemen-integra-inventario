package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsumoOPDTO {
    private Long idInsumo;
    private String nombre;
    private String unidad;
    private BigDecimal cantidadRequerida;
    private BigDecimal cantidadConsumida;
    private BigDecimal faltante;
}
