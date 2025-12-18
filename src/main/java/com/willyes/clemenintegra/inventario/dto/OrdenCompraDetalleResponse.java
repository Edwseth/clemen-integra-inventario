package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrdenCompraDetalleResponse {
    public Long id;
    public BigDecimal cantidad;
    public BigDecimal valorUnitario;
    public BigDecimal valorTotal;
    public BigDecimal iva;
    public BigDecimal cantidadRecibida;
    public BigDecimal cantidadPendiente;
    public LocalDate fechaNecesidad;

    public ProductoMiniDTO producto;

}
