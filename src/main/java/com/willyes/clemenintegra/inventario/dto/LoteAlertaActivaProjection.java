package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LoteAlertaActivaProjection {
    Long getLoteProductoId();
    String getCodigoLote();
    LocalDateTime getFechaVencimiento();
    Long getProductoId();
    String getNombreProducto();
    String getCodigoSku();
    Long getAlmacenId();
    String getNombreAlmacen();
    BigDecimal getStockActual();
}
