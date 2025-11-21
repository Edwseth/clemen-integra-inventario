package com.willyes.clemenintegra.produccion.dto;

import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BatchRecordDTO {
    public OpDTO op;
    public FormulaDTO formula;
    public List<ConsumoDTO> consumos;
    public List<ReservaDTO> reservas;
    public List<CierreDTO> cierres;
    public ProduccionFinalDTO produccionFinal;
    public LotePTDTO loteProductoTerminado;
    public CalidadDTO calidad;
    public List<ControlProcesoDTO> controlesProceso;
    public List<ControlEmpaqueDTO> controlesEmpaque;
    public List<ObservacionProcesoDTO> observacionesProceso;
    public EstadoBatchRecord estadoBatchRecord;
    public String revisadoPorNombre;
    public LocalDateTime fechaRevision;
    public String observacionesCalidad;

    public static class OpDTO {
        public Long id;
        public String codigoOrden;
        public String loteProduccion;
        public Long productoId;
        public String productoNombre;
        public String codigoSku;
        public String presentacion;
        public Integer cantidadProgramada;
        public Integer cantidadProducida;
        public BigDecimal porcentajeCumplimiento;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
        public String estado;
        public String responsableNombre;
    }

    public static class FormulaDTO {
        public String version;
        public List<DetalleFormulaDTO> detalles;
    }

    public static class DetalleFormulaDTO {
        public Long insumoId;
        public String codigoSku;
        public String nombre;
        public String unidad;
        public BigDecimal cantidadNecesaria;
        public boolean obligatorio;
    }

    public static class ConsumoDTO {
        public String tipoMovimiento;
        public String clasificacionMovimiento;
        public Long productoId;
        public String codigoSku;
        public String nombreProducto;
        public Long loteId;
        public String codigoLote;
        public String almacenOrigen;
        public BigDecimal cantidad;
        public String unidad;
        public LocalDateTime fechaMovimiento;
    }

    public static class ReservaDTO {
        public Long insumoId;
        public Long loteId;
        public String codigoLote;
        public BigDecimal cantidadReservada;
        public BigDecimal cantidadConsumida;
        public String estado;
    }

    public static class CierreDTO {
        public Long id;
        public String tipo;
        public Integer cantidad;
        public String turno;
        public String observacion;
        public LocalDateTime fechaCierre;
        public String usuarioNombre;
    }

    public static class ProduccionFinalDTO {
        public Integer unidadesProducidas;
        public Integer unidadesAprobadas;
        public Integer unidadesRechazadas;
        public BigDecimal rendimientoCalculado;
    }

    public static class LotePTDTO {
        public Long loteId;
        public String codigoLote;
        public String estado;
        public LocalDateTime fechaFabricacion;
        public LocalDateTime fechaVencimiento;
        public LocalDateTime fechaLiberacion;
        public String usuarioLiberador;
    }

    public static class CalidadDTO {
        public List<EvaluacionDTO> evaluaciones;
        public List<RetencionDTO> retenciones;
    }

    public static class EvaluacionDTO {
        public Long id;
        public String tipoEvaluacion;
        public String resultado;
        public String observaciones;
        public LocalDateTime fechaEvaluacion;
        public String evaluador;
    }

    public static class RetencionDTO {
        public String estado;
        public String motivo;
        public LocalDateTime fechaRetencion;
        public LocalDateTime fechaLiberacion;
        public String aprobador;
    }

    public static class ControlProcesoDTO {
        public Long id;
        public String etapa;
        public String parametro;
        public String valorMedido;
        public String unidad;
        public Boolean cumple;
        public String observaciones;
        public String evaluadoPor;
        public LocalDateTime fechaRegistro;
    }

    public static class ControlEmpaqueDTO {
        public Long id;
        public String parametro;
        public String valorMedido;
        public String unidad;
        public Boolean cumple;
        public String observaciones;
        public String evaluadoPor;
        public LocalDateTime fechaRegistro;
    }

    public static class ObservacionProcesoDTO {
        public Long id;
        public String tipo;
        public String descripcion;
        public String registradoPor;
        public LocalDateTime fechaRegistro;
    }
}
