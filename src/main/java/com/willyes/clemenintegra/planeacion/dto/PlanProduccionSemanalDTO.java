package com.willyes.clemenintegra.planeacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanProduccionSemanalDTO {
    private Long id;
    private LocalDate semanaInicio;
    private LocalDate semanaFin;
    private String estado;
    private String comentarios;
    private Long creadoPorId;
    private List<PlanProduccionDetalleDTO> detalles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanProduccionDetalleDTO {
        private Long id;
        private Long productoId;
        private BigDecimal cantidadPlanificada;
        private Long unidadMedidaId;
        private Integer prioridad;
        private String origenDemanda;
        private String observacion;
    }
}
