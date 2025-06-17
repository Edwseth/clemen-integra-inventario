package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaProductoMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "tipo", source = "tipo")
    CategoriaProductoResponseDTO toDto(CategoriaProducto categoria);
}
