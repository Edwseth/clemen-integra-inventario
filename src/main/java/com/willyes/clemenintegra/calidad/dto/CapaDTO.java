package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapaDTO {

    private Long id;

    private String noConformidadCodigo;

    @NotNull(message = "La no conformidad es obligatoria")
    private Long noConformidadId;

    @NotNull(message = "El tipo de CAPA es obligatorio")
    private TipoCapa tipo;

    private String responsableNombre;

    @NotNull(message = "El responsable es obligatorio")
    private Long responsableId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaCierre;

    private LocalDateTime fechaLimite;

    private EstadoCapa estado;

    @JsonAlias("descripcion")
    private String observaciones;

    @Builder.Default
    private List<CapaArchivoDTO> archivosAdjuntos = List.of();
}
