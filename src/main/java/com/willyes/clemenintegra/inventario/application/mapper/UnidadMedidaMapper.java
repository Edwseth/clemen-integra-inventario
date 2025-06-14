package com.willyes.clemenintegra.inventario.application.mapper;

import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.domain.model.UnidadMedida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnidadMedidaMapper {

    @Mapping(target = "id", ignore = true)
    UnidadMedida toEntity(UnidadMedidaRequestDTO dto);

    UnidadMedidaResponseDTO toDTO(UnidadMedida unidad);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UnidadMedidaRequestDTO dto, @MappingTarget UnidadMedida unidad);
}
