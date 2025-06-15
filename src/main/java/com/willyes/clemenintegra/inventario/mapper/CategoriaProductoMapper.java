package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaProductoMapper {
    CategoriaProductoResponseDTO toDto(CategoriaProducto categoria);
}
