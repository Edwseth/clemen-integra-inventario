package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public class OrdenCompraDetalleRequest {
    public BigDecimal cantidad;
    public BigDecimal valorUnitario;
    public BigDecimal valorTotal;
    public BigDecimal iva;
    public BigDecimal cantidadRecibida;
    public Long ordenCompraId;
    public Long productoId;
}
