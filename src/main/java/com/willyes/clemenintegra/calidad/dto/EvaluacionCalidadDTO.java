package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadDTO {

    private Long id;

    @NotNull(message = "El resultado es obligatorio")
    private ResultadoEvaluacion resultado;

    @NotNull(message = "La fecha de evaluación es obligatoria")
    private LocalDateTime fechaEvaluacion;

    @NotNull(message = "Las observaciones son obligatorias")
    private String observaciones;

    private String archivoAdjunto;

    @NotNull(message = "El lote es obligatorio")
    private Long loteProductoId;

    @NotNull(message = "El evaluador es obligatorio")
    private Long usuarioEvaluadorId;
}


