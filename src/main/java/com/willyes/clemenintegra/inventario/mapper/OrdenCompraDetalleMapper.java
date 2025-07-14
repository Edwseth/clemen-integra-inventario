package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.dto.ProductoMiniDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMiniDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrdenCompraDetalleMapper {

    @Mapping(target = "producto", source = "producto", qualifiedByName = "mapProductoMini")
    OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity);

    @Named("mapProductoMini")
    default ProductoMiniDTO mapProductoMini(com.willyes.clemenintegra.inventario.model.Producto producto) {
        if (producto == null) return null;
        UnidadMiniDTO unidadDTO = new UnidadMiniDTO(
                producto.getUnidadMedida() != null ? producto.getUnidadMedida().getSimbolo() : null
        );
        return new ProductoMiniDTO(
                producto.getId().longValue(),
                producto.getNombre(),
                unidadDTO
        );
    }
}



