package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleDiagnosticoInsumoDTO {
    private Long productoId;
    private String nombre;
    private BigDecimal requerido;
    private BigDecimal stockLibreFefo;
    private BigDecimal faltante;
    private boolean suficiente;
    private Integer maximoProducible;
    private String unidadInsumoSimbolo;
    private String unidadInsumoNombre;
    private String unidadInsumoNombrePlural;
    private List<Long> almacenesPreferidos;
}
