package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MotivoMovimientoMapper {
    MotivoMovimientoResponseDTO toDTO(MotivoMovimiento motivo);
}
