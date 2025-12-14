package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IndicadoresProduccionResponseDTO {

    private long totalOrdenesPeriodo;
    private long ordenesEnTiempo;
    private long ordenesRetrasadas;
    private double porcentajeCumplimiento;
    private BigDecimal cantidadTotalPlanificada;
    private BigDecimal cantidadTotalProducida;
    private long ordenesAbiertasConVencimientoVencido;
}
