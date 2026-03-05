package com.willyes.clemenintegra.inventario.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ConteoAjusteReporteProjection {
    Long getConteoId();
    LocalDateTime getFechaConteo();
    LocalDateTime getFechaAplicacion();
    Long getAlmacenId();
    String getAlmacenNombre();
    Long getProductoId();
    String getProductoSku();
    String getProductoNombre();
    Long getLoteId();
    String getLoteCodigo();
    BigDecimal getStockAntes();
    BigDecimal getConteoFisico();
    BigDecimal getDiferencia();
    String getTipoAjuste();
    BigDecimal getStockFinal();
    Long getUsuarioConteoId();
    String getUsuarioConteoNombre();
}
