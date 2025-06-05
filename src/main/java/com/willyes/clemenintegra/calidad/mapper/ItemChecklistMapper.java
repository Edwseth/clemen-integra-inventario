package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ItemChecklistDTO;
import com.willyes.clemenintegra.calidad.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ItemChecklistMapper {

    public ItemChecklistDTO toDTO(ItemChecklist entity) {
        return ItemChecklistDTO.builder()
                .id(entity.getId())
                .checklistId(entity.getChecklist().getId())
                .descripcionItem(entity.getDescripcionItem())
                .cumple(entity.getCumple())
                .observaciones(entity.getObservaciones())
                .fechaRevision(entity.getFechaRevision())
                .revisadoPorId(entity.getRevisadoPor().getId())
                .build();
    }

    public ItemChecklist toEntity(ItemChecklistDTO dto,
                                  ChecklistCalidad checklist,
                                  Usuario revisadoPor) {
        return ItemChecklist.builder()
                .id(dto.getId())
                .checklist(checklist)
                .descripcionItem(dto.getDescripcionItem())
                .cumple(dto.getCumple())
                .observaciones(dto.getObservaciones())
                .fechaRevision(dto.getFechaRevision())
                .revisadoPor(revisadoPor)
                .build();
    }
}

