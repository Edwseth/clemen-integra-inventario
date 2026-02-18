package com.willyes.clemenintegra.inventario.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReporteComprasRowProjection {

    String getOcCodigo();

    String getEstado();

    String getProductoCodigo();

    String getProductoNombre();

    String getUdm();

    BigDecimal getCantidad();

    LocalDateTime getFechaOc();

    LocalDate getFechaPactada();

    LocalDate getFechaRecepcion();

    String getProveedorNombre();

    String getCondicionesPago();

    BigDecimal getPrecioUnitario();

    BigDecimal getIva();
}
