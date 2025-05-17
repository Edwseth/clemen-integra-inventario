package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapaDTO {

    private Long id;

    @NotNull(message = "La no conformidad es obligatoria")
    private Long noConformidadId;

    @NotNull(message = "El tipo de CAPA es obligatorio")
    private TipoCapa tipo;

    @NotNull(message = "El responsable es obligatorio")
    private Long responsableId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaCierre;

    @NotNull(message = "El estado es obligatorio")
    private EstadoCapa estado;

    private String observaciones;
}

