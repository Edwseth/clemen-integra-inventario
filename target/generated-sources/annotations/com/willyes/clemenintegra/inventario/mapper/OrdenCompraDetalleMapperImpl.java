package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T18:54:38-0500",
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

        ordenCompraDetalleResponse.productoNombre = entity.getProducto() != null ? entity.getProducto().getNombre() : null;

        return ordenCompraDetalleResponse;
    }
}
