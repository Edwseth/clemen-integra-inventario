package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadRequestDTO {

    @NotNull(message = "El resultado es obligatorio")
    private ResultadoEvaluacion resultado;

    @NotNull(message = "El tipo de evaluación es obligatorio")
    private TipoEvaluacion tipoEvaluacion;

    @NotNull(message = "Las observaciones son obligatorias")
    private String observaciones;

    private List<String> archivosAdjuntos;

    @NotNull(message = "El lote es obligatorio")
    private Long loteProductoId;

    private Long usuarioEvaluadorId;
}



