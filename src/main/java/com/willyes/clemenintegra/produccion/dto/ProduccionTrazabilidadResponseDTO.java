package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduccionTrazabilidadResponseDTO {

    private OpDTO op;
    private List<EtapaDTO> etapas;
    private List<ConsumoDTO> consumos;
    private List<MovimientoDTO> movimientos;
    private List<LoteResultanteDTO> lotesResultantes;
    private List<CierreDTO> cierres;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpDTO {
        private Long id;
        private String codigoOrden;
        private Long productoId;
        private String nombreProducto;
        private String codigoSku;
        private String loteProduccion;
        private String estado;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EtapaDTO {
        private Long id;
        private Integer secuencia;
        private String nombreEtapa;
        private String estado;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private Long usuarioId;
        private String usuarioNombre;
        private ChecklistResumenDTO checklist;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistResumenDTO {
        private Integer total;
        private Integer obligatoriosPendientes;
        private Boolean completo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumoDTO {
        private Long movimientoId;
        private Long etapaId;
        private String tipoMovimiento;
        private String clasificacion;
        private Long productoId;
        private String codigoSku;
        private String nombreProducto;
        private Long loteId;
        private String codigoLote;
        private Long almacenOrigenId;
        private String almacenOrigenNombre;
        private BigDecimal cantidad;
        private LocalDateTime fechaMovimiento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimientoDTO {
        private Long movimientoId;
        private Long etapaId;
        private String tipoMovimiento;
        private String clasificacion;
        private Long loteId;
        private String codigoLote;
        private String almacenOrigen;
        private String almacenDestino;
        private BigDecimal cantidad;
        private LocalDateTime fechaMovimiento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteResultanteDTO {
        private Long loteId;
        private String codigoLote;
        private String estado;
        private Long almacenId;
        private String almacenNombre;
        private LocalDateTime fechaFabricacion;
        private LocalDateTime fechaVencimiento;
        private Long lotePsOrigenId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CierreDTO {
        private Long id;
        private String tipoCierre;
        private LocalDateTime fecha;
        private Long usuarioId;
        private String usuarioNombre;
        private String observacion;
    }
}
