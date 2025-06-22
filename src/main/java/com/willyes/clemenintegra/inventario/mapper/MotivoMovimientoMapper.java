package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MotivoMovimientoMapper {

    @Mapping(target = "motivo", expression = "java(motivo.getMotivo().name())")
    MotivoMovimientoResponseDTO toDTO(MotivoMovimiento motivo);
}
