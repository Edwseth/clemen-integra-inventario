package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.model.ChecklistCalidad;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ChecklistCalidadMapper {

    public ChecklistCalidadDTO toDTO(ChecklistCalidad entity) {
        return ChecklistCalidadDTO.builder()
                .id(entity.getId())
                .tipoChecklist(entity.getTipoChecklist())
                .fechaCreacion(entity.getFechaCreacion())
                .descripcionGeneral(entity.getDescripcionGeneral())
                .creadoPorId(entity.getCreadoPor().getId())
                .build();
    }

    public ChecklistCalidad toEntity(ChecklistCalidadDTO dto, Usuario creadoPor) {
        return ChecklistCalidad.builder()
                .id(dto.getId())
                .tipoChecklist(dto.getTipoChecklist())
                .fechaCreacion(dto.getFechaCreacion())
                .descripcionGeneral(dto.getDescripcionGeneral())
                .creadoPor(creadoPor)
                .build();
    }
}

