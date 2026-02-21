package com.willyes.clemenintegra.inventario.regularizacion.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VariacionRegularizacionProjection {
    Long getRegularizacionId();

    Long getOrdenProduccionId();

    BigDecimal getCantidadProgramada();

    BigDecimal getCantidadReal();

    BigDecimal getDiferencia();

    Boolean getAjustarPt();

    String getDocumentoReferencia();

    String getObservaciones();

    Long getUsuarioId();

    LocalDateTime getFechaIngreso();

    Boolean getTieneDetalle();
}
