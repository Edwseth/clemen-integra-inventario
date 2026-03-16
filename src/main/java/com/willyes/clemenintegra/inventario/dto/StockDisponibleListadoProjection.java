package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockDisponibleListadoProjection {
    String getSku();
    String getNombre();
    String getCategoria();
    BigDecimal getCantidadActual();
    String getLote();
    LocalDateTime getFechaVencimiento();
    String getAlmacenNombre();
    String getUbicacionCodigo();
    String getUbicacionDescripcion();
}

