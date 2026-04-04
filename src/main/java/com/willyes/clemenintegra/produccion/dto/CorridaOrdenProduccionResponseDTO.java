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
public class CorridaOrdenProduccionResponseDTO {
    private Long planId;
    private Long planDetalleId;
    private Long productoId;
    private Integer semanasVigencia;
    private BigDecimal maximoPorOp;
    private BigDecimal cantidadPlanificada;
    private BigDecimal cantidadProgramadaPrevia;
    private BigDecimal cantidadProgramadaEnCorrida;
    private BigDecimal cantidadTotalProgramadaEnOp;
    private BigDecimal cantidadPendienteRestante;
    private int totalOpPrevias;
    private int totalOpCreadas;
    private int totalOpAsociadas;
    private List<Long> opIdsCreadas;
    private List<String> opCodigosCreadas;
    private String idempotencyKey;
    private String mensaje;
}
