package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class CapaMapper {

    public CapaDTO toDTO(Capa entity) {
        return CapaDTO.builder()
                .id(entity.getId())
                .noConformidadId(entity.getNoConformidad().getId())
                .tipo(entity.getTipo())
                .responsableId(entity.getResponsable().getId())
                .fechaInicio(entity.getFechaInicio())
                .fechaCierre(entity.getFechaCierre())
                .estado(entity.getEstado())
                .observaciones(entity.getObservaciones())
                .build();
    }

    public Capa toEntity(CapaDTO dto,
                         com.willyes.clemenintegra.calidad.model.NoConformidad noConformidad,
                         Usuario responsable) {
        return Capa.builder()
                .id(dto.getId())
                .noConformidad(noConformidad)
                .tipo(dto.getTipo())
                .responsable(responsable)
                .fechaInicio(dto.getFechaInicio())
                .fechaCierre(dto.getFechaCierre())
                .estado(dto.getEstado())
                .observaciones(dto.getObservaciones())
                .build();
    }
}

