package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemChecklistDTO {

    private Long id;

    @NotNull(message = "El checklist es obligatorio")
    private Long checklistId;

    @NotNull(message = "La descripción del ítem es obligatoria")
    private String descripcionItem;

    private Boolean cumple = false;

    private String observaciones;

    @NotNull(message = "La fecha de revisión es obligatoria")
    private LocalDateTime fechaRevision;

    @NotNull(message = "El revisor es obligatorio")
    private Long revisadoPorId;
}

