package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UnidadMedidaMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "simbolo", source = "simbolo")
    UnidadMedidaResponseDTO toDto(UnidadMedida unidad);
}
