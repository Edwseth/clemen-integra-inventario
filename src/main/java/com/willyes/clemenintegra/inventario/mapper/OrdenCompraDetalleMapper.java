package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrdenCompraDetalleMapper {

    @Mapping(target = "productoNombre", source = "producto.nombre")
    OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity);
}
