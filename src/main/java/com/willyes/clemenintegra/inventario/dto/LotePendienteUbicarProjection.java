package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LotePendienteUbicarProjection {
    Long getLoteId();

    String getCodigoLote();

    Long getProductoId();

    String getNombreProducto();

    String getTipoProducto();

    BigDecimal getStockDisponible();

    LocalDateTime getFechaVencimiento();

    String getEstado();

    Long getAlmacenIdActual();

    String getNombreAlmacenActual();
}
