package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T19:27:31-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class OrdenCompraDetalleMapperImpl implements OrdenCompraDetalleMapper {

    @Override
    public OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity) {
        if ( entity == null ) {
            return null;
        }

        OrdenCompraDetalleResponse ordenCompraDetalleResponse = new OrdenCompraDetalleResponse();

        ordenCompraDetalleResponse.id = entity.getId();
        ordenCompraDetalleResponse.cantidad = entity.getCantidad();
        ordenCompraDetalleResponse.valorUnitario = entity.getValorUnitario();
        ordenCompraDetalleResponse.valorTotal = entity.getValorTotal();
        ordenCompraDetalleResponse.iva = entity.getIva();
        ordenCompraDetalleResponse.cantidadRecibida = entity.getCantidadRecibida();

        ordenCompraDetalleResponse.productoNombre = entity.getProducto() != null ? entity.getProducto().getNombre() : null;

        return ordenCompraDetalleResponse;
    }
}
