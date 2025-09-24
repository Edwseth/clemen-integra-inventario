package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección para la consulta FEFO de lotes disponibles.
 */
public interface LoteFefoDisponibleProjection {
    Long getLoteProductoId();
    String getCodigoLote();
    BigDecimal getStockLote();
    LocalDateTime getFechaVencimiento();
    Long getAlmacenId();
    String getNombreAlmacen();
}
