package com.willyes.clemenintegra.inventario.application.mapper;

import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.domain.model.CategoriaProducto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoriaProductoMapper {

    @Mapping(target = "id", ignore = true)
    CategoriaProducto toEntity(CategoriaProductoRequestDTO dto);

    CategoriaProductoResponseDTO toDTO(CategoriaProducto categoria);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(CategoriaProductoRequestDTO dto, @MappingTarget CategoriaProducto categoria);
}
