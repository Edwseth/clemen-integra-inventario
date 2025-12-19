package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
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
public class AuditoriaLoteResponseDTO {

    private Long loteId;
    private String codigoLote;
    private String nombreProducto;
    private String categoriaProducto;
    private String tipoAnalisisCalidad;
    private String estadoLote;
    private LocalDateTime fechaFabricacion;
    private LocalDateTime fechaVencimiento;
    private BigDecimal stockLote;
    private String nombreAlmacenActual;
    private String ubicacionAlmacenActual;

    private DatosLoteDTO datosLote;
    private CalidadLoteAuditoriaDTO calidad;

    private EstadoCalidadLoteResponseDTO estadoCalidad;

    private List<EvaluacionResumenDTO> evaluaciones;
    private List<IncidenteDTO> incidentes;
    private List<RetencionDTO> retenciones;
    private CondicionUsoDTO condicionUsoActiva;
    private List<MovimientoDTO> movimientos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluacionResumenDTO {
        private Long id;
        private TipoEvaluacion tipoEvaluacion;
        private String resultado;
        private LocalDateTime fechaEvaluacion;
        private String usuarioEvaluador;
        private boolean tieneAdjuntos;
        private boolean tieneResultadosMicro;
        private Boolean conformeMicro;
        private boolean pdfMicroDisponible;
        private List<EvaluacionAdjuntoDTO> adjuntos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluacionAdjuntoDTO {
        private String nombreArchivo;
        private String nombreVisible;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosLoteDTO {
        private String codigoLote;
        private String productoNombre;
        private String estado;
        private LocalDateTime fechaIngreso;
        private LocalDateTime fechaVencimiento;
        private String almacen;
        private String tipoAnalisisRequerido;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalidadLoteAuditoriaDTO {
        private String tipoAnalisisRequerido;
        private DisciplinaCalidadDTO fisico;
        private DisciplinaCalidadDTO quimicoMicrobiologico;
        private DisciplinaCalidadDTO microbiologico;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisciplinaCalidadDTO {
        private boolean requerido;
        private String estado;
        private String resultado;
        private LocalDateTime fechaUltimaEvaluacion;
        private String evaluador;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidenteDTO {
        private Long id;
        private String codigo;
        private TipoIncidente tipoIncidente;
        private SeveridadNoConformidad severidad;
        private EstadoNoConformidad estado;
        private LocalDateTime fechaApertura;
        private LocalDateTime fechaCierre;
        private boolean tieneCapa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetencionDTO {
        private Long id;
        private MotivoRetencion motivo;
        private String descripcion;
        private String estado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondicionUsoDTO {
        private Long id;
        private TipoCondicionUso tipo;
        private LocalDateTime parametroFecha;
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimientoDTO {
        private Long id;
        private LocalDateTime fechaMovimiento;
        private TipoMovimiento tipoMovimiento;
        private ClasificacionMovimientoInventario clasificacion;
        private BigDecimal cantidad;
        private String almacenOrigenNombre;
        private String almacenDestinoNombre;
        private String almacenOrigen;
        private String almacenDestino;
        private String motivoMovimientoNombre;
        private String registradoPorNombre;
        private String ordenProduccionCodigo;
    }
}
