package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;

public class OrdenCompraDetalleResponse {
    public Long id;
    public BigDecimal cantidad;
    public BigDecimal valorUnitario;
    public BigDecimal valorTotal;
    public BigDecimal iva;
    public BigDecimal cantidadRecibida;
    public String productoNombre;
    public String productoUnidadSimbolo;

}
