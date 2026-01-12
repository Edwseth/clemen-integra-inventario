package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;

import java.time.LocalDateTime;

public interface NoConformidadListadoProjection {

    Long getId();

    String getCodigo();

    OrigenNoConformidad getOrigen();

    SeveridadNoConformidad getSeveridad();

    TipoIncidente getTipoIncidente();

    EstadoNoConformidad getEstado();

    String getDescripcion();

    String getEvidencia();

    LocalDateTime getFechaRegistro();

    LocalDateTime getFechaCierre();

    Long getUsuarioReportaId();

    Long getLoteId();

    Integer getProductoId();

    Long getEvaluacionId();

    String getCodigoLote();

    String getReportadoPorNombre();

    String getProductoNombre();

    Long getCreadoPor();

    Long getActualizadoPor();

    LocalDateTime getActualizadoEn();
}
