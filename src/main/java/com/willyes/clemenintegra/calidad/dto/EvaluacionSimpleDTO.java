package com.willyes.clemenintegra.calidad.dto;

import lombok.*;
import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<ArchivoEvaluacionDTO> archivosAdjuntos;
}
