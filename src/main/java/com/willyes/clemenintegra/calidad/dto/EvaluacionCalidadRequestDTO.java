package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadRequestDTO {

    @NotNull(message = "El resultado es obligatorio")
    private ResultadoEvaluacion resultado;

    @NotNull(message = "Las observaciones son obligatorias")
    private String observaciones;

    private String archivoAdjunto;

    @NotNull(message = "El lote es obligatorio")
    private Long loteProductoId;

    private Long usuarioEvaluadorId;
}



