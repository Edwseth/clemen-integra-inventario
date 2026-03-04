package com.willyes.clemenintegra.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrdenCompraConDetallesResponse {
    public Long id;
    public String codigoOrden;
    public String estado;
    public String tipo;
    public LocalDateTime fechaOrden;
    public String observaciones;
    public ProveedorMinResponse proveedor;
    public BigDecimal descuento;
    public List<OrdenCompraDetalleResponse> detalles;
    public BigDecimal totalPedido;
    public BigDecimal totalRecibido;
    public BigDecimal totalPendiente;
    public BigDecimal porcentajeAvance;
    public java.time.LocalDate fechaCompromisoEntrega;
}
