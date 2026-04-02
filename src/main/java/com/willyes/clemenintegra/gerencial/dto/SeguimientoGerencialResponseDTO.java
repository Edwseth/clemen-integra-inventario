package com.willyes.clemenintegra.gerencial.dto;

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
/**
 * Contrato consolidado del seguimiento gerencial V1.
 *
 * <p>{@code summary}: vista agregada del plan semanal completo.
 * <br>{@code items[]}: vista de detalle por ítem de plan (1:1 con plan_produccion_detalle).
 */
public class SeguimientoGerencialResponseDTO {

    private SummaryDTO summary;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDTO {
        private Long planSemanalId;
        private LocalDate semanaInicio;
        private LocalDate semanaFin;
        private String estadoPlan;
        private int totalItems;
        private int itemsNoIniciados;
        private int itemsEnProceso;
        private int itemsEnRiesgo;
        private int itemsBloqueados;
        private int itemsCompletados;
        private int itemsCerradosConNovedad;
        /** % global calculado con sumatoria ejecutada / sumatoria planificada de todos los items. */
        private BigDecimal porcentajeCumplimientoGeneral;
        private int totalOpGeneradas;
        private int totalOpCerradas;
        private int totalAlertas;
        private int totalItemsConIntervencionRequerida;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private Long planDetalleId;
        private Long planSemanalId;
        private Long productoId;
        private String sku;
        private String nombreProducto;
        private BigDecimal cantidadPlanificada;
        private String unidadMedida;
        private Integer prioridad;
        /** Estado semántico gerencial derivado por reglas de bloqueo, ejecución y cierre. */
        private String estadoGerencial;
        private String etapaActual;
        private BloqueoPrincipalDTO bloqueoPrincipal;
        private ResponsableActualDTO responsableActual;
        private int opGeneradas;
        private int opCerradas;
        private BigDecimal cantidadEjecutada;
        /** % cumplimiento por ítem: cantidadEjecutada / cantidadPlanificada * 100. */
        private BigDecimal porcentajeCumplimiento;
        private boolean requiereIntervencion;
        private List<AlertaDTO> alertas;
        private MetadatosTrazabilidadDTO metadatosTrazabilidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BloqueoPrincipalDTO {
        private String codigo;
        private String mensaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsableActualDTO {
        private String tipo;
        private Long id;
        private String nombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaDTO {
        private String codigo;
        private String severidad;
        private String origen;
        private String mensaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadatosTrazabilidadDTO {
        private FormulaDTO formula;
        private MrpDTO mrp;
        private OperacionDTO operacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaDTO {
        private Long formulaId;
        private String estado;
        private String version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpDTO {
        private Long corridaMrpId;
        private String estadoCorrida;
        private Integer sugerenciasPendientes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperacionDTO {
        private List<Long> opIds;
        private List<Long> loteIds;
        private List<String> batchRecordEstados;
    }
}
