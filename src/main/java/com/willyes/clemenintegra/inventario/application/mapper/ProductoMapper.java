package com.willyes.clemenintegra.inventario.application.mapper;

import com.willyes.clemenintegra.inventario.application.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.domain.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "unidadMedida", ignore = true)
    @Mapping(target = "categoriaProducto", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    Producto toEntity(ProductoRequestDTO dto);

    @Mapping(target = "unidadMedida", source = "unidadMedida.nombre")
    @Mapping(target = "categoria", source = "categoriaProducto.nombre")
    ProductoResponseDTO toDTO(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "unidadMedida", ignore = true)
    @Mapping(target = "categoriaProducto", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    void updateEntityFromDto(ProductoRequestDTO dto, @MappingTarget Producto producto);
}
