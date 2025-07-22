package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadResponseDTO {

    private Long id;
    private ResultadoEvaluacion resultado;
    private LocalDateTime fechaEvaluacion;
    private String observaciones;
    private String archivoAdjunto;

    private String nombreLote;         // Referencia al lote evaluado
    private String nombreProducto;     // Nombre del producto evaluado
    private String nombreEvaluador;    // Nombre del usuario evaluador
}
