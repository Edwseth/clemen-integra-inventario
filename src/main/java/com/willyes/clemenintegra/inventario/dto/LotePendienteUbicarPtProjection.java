package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LotePendienteUbicarPtProjection {
    Long getLoteId();

    String getCodigoLote();

    Integer getProductoId();

    String getNombreProducto();

    BigDecimal getStockDisponible();

    LocalDateTime getFechaVencimiento();

    String getEstado();

    Integer getAlmacenIdActual();

    String getNombreAlmacenActual();
}
