package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionSimpleDTO {
    private String tipoEvaluacion;
    private String resultado;
    private String nombreEvaluador;
    private String observaciones;
    private LocalDateTime fechaEvaluacion;
}
