package com.willyes.clemenintegra.inventario.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InventarioValorizadoRowProjection {
    String getSku();
    String getNombre();
    String getUdm();
    String getLote();
    LocalDate getVence();
    String getUbicacionAlmacen();
    BigDecimal getStock();
    BigDecimal getCostoUnitarioMaterial();
    BigDecimal getValorTotal();
}
