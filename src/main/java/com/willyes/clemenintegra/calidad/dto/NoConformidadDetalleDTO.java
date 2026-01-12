package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoConformidadDetalleDTO {

    private final Long id;
    private final String codigo;
    private final OrigenNoConformidad origen;
    private final SeveridadNoConformidad severidad;
    private final TipoIncidente tipoIncidente;
    private final EstadoNoConformidad estado;
    private final String descripcion;
    private final String evidencia;
    private final LocalDateTime fechaRegistro;
    private final LocalDateTime fechaCierre;
    private final Long usuarioReportaId;
    private final Long loteId;
    private final Long productoId;
    private final Long evaluacionId;
    private final String codigoLote;
    private final String reportadoPorNombre;
    private final String productoNombre;
    private final Long creadoPor;
    private final Long actualizadoPor;
    private final LocalDateTime actualizadoEn;

    public NoConformidadDetalleDTO(Long id,
                                   String codigo,
                                   OrigenNoConformidad origen,
                                   SeveridadNoConformidad severidad,
                                   TipoIncidente tipoIncidente,
                                   EstadoNoConformidad estado,
                                   String descripcion,
                                   String evidencia,
                                   LocalDateTime fechaRegistro,
                                   LocalDateTime fechaCierre,
                                   Long usuarioReportaId,
                                   Long loteId,
                                   Long productoId,
                                   Long evaluacionId,
                                   String codigoLote,
                                   String reportadoPorNombre,
                                   String productoNombre,
                                   Long creadoPor,
                                   Long actualizadoPor,
                                   LocalDateTime actualizadoEn) {
        this.id = id;
        this.codigo = codigo;
        this.origen = origen;
        this.severidad = severidad;
        this.tipoIncidente = tipoIncidente;
        this.estado = estado;
        this.descripcion = descripcion;
        this.evidencia = evidencia;
        this.fechaRegistro = fechaRegistro;
        this.fechaCierre = fechaCierre;
        this.usuarioReportaId = usuarioReportaId;
        this.loteId = loteId;
        this.productoId = productoId;
        this.evaluacionId = evaluacionId;
        this.codigoLote = codigoLote;
        this.reportadoPorNombre = reportadoPorNombre;
        this.productoNombre = productoNombre;
        this.creadoPor = creadoPor;
        this.actualizadoPor = actualizadoPor;
        this.actualizadoEn = actualizadoEn;
    }
}
