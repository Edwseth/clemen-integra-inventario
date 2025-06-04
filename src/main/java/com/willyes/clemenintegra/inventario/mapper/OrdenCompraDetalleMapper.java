package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;

public class OrdenCompraDetalleMapper {

    public static OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity) {
        OrdenCompraDetalleResponse dto = new OrdenCompraDetalleResponse();
        dto.id = entity.getId();
        dto.cantidad = entity.getCantidad();
        dto.valorUnitario = entity.getValorUnitario();
        dto.valorTotal = entity.getValorTotal();
        dto.iva = entity.getIva();
        dto.cantidadRecibida = entity.getCantidadRecibida();
        dto.productoNombre = entity.getProducto().getNombre();
        return dto;
    }
}
