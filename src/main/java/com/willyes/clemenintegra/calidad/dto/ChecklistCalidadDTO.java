package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistCalidadDTO {

    private Long id;

    @NotNull(message = "El tipo de checklist es obligatorio")
    private TipoChecklist tipoChecklist;

    @NotNull(message = "La fecha de creación es obligatoria")
    private LocalDateTime fechaCreacion;

    private String descripcionGeneral;

    @NotNull(message = "El usuario creador es obligatorio")
    private Long creadoPorId;
}

