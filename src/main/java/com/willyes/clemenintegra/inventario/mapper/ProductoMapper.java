package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "unidadMedida", source = "unidadMedida.nombre")
    @Mapping(target = "categoria", source = "categoriaProducto.nombre")
    ProductoResponseDTO toDto(Producto producto);
}
