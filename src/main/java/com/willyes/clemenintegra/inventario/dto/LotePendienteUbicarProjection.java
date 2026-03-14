package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public interface LotePendienteUbicarProjection {
    Long getLoteId();

    String getCodigoLote();

    Integer getProductoId();

    String getNombreProducto();

    String getTipoProducto();

    String getNombreAlmacen();

    BigDecimal getStockDisponible();
}

