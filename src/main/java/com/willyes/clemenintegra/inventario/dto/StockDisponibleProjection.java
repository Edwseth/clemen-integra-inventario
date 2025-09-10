package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public interface StockDisponibleProjection {
    Long getProductoId();
    BigDecimal getStockDisponible();
}
