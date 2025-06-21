package com.willyes.clemenintegra.inventario.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrdenCompraConDetallesResponse {
    public Long id;
    public String codigoOrden;
    public String estado;
    public LocalDateTime fechaOrden;
    public String observaciones;
    public ProveedorMinResponse proveedor;
    public List<OrdenCompraDetalleResponse> detalles;
}
