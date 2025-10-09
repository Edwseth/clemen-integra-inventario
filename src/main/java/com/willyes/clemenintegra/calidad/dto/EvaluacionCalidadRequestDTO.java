package com.willyes.clemenintegra.calidad.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCondicionDTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Información de archivos adjuntos para la evaluación.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluacionCalidadRequestDTO {

    @NotNull(message = "El resultado es obligatorio")
    private ResultadoEvaluacion resultado;

    @NotNull(message = "El tipo de evaluación es obligatorio")
    private TipoEvaluacion tipoEvaluacion;

    @NotNull(message = "Las observaciones son obligatorias")
    private String observaciones;

    private List<ArchivoEvaluacionDTO> archivosAdjuntos;

    @NotNull(message = "El lote es obligatorio")
    private Long loteProductoId;

    private Long usuarioEvaluadorId;

    private EvaluacionCondicionDTO condicion;

    private SeveridadNoConformidad severidadNc;
}



