package com.willyes.clemenintegra.planeacion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.willyes.clemenintegra.inventario.dto.ProductoResumenDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
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

    @JsonAlias({"fechaInicio"})
    private LocalDate semanaInicio;

    @JsonAlias({"fechaFin"})
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
        private ProductoResumenDTO producto;
        private UnidadMedidaResponseDTO unidadMedida;
        private String tipoProducto;
        private Boolean esHomeopatico;
        private Integer totalOpAsociadas;
        private BigDecimal cantidadTotalProgramadaEnOp;
        private BigDecimal cantidadPendiente;
        private List<Long> opIds;
        private List<String> opCodigos;
        private String modoAccionSugerido;
    }
}
