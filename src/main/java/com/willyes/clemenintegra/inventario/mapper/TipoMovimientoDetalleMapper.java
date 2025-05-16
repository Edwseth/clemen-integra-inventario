package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import org.springframework.stereotype.Component;

@Component
public class TipoMovimientoDetalleMapper {

    public TipoMovimientoDetalleDTO toDTO(TipoMovimientoDetalle entity) {
        return TipoMovimientoDetalleDTO.builder()
                .id(entity.getId())
                .descripcion(entity.getDescripcion())
                .build();
    }

    public TipoMovimientoDetalle toEntity(TipoMovimientoDetalleDTO dto) {
        return TipoMovimientoDetalle.builder()
                .id(dto.getId())
                .descripcion(dto.getDescripcion())
                .build();
    }
}
