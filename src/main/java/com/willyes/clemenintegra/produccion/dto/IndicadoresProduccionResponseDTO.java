package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * Lista opcional de alertas de OP próximas a vencer o retrasadas para el rango consultado.
     */
    private List<AlertaOrdenProduccionDTO> alertas;

    /**
     * Ventana de días utilizada para calcular las alertas. Solo informativo.
     */
    private Integer diasAlerta;
}
