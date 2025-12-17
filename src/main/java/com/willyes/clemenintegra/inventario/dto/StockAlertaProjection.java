package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public interface StockAlertaProjection {
    Long getProductoId();
    String getNombreProducto();
    String getCodigoSku();
    Long getAlmacenId();
    String getNombreAlmacen();
    BigDecimal getStockMinimo();
    BigDecimal getStockMaximoPlaneacion();
    BigDecimal getStockActual();
}
