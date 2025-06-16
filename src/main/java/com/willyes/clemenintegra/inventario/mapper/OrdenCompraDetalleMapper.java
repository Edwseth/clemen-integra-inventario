package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrdenCompraDetalleMapper {

    @Mapping(target = "productoNombre", expression = "java(entity.getProducto() != null ? entity.getProducto().getNombre() : null)")
    OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity);
}



