package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import lombok.*;
import java.util.List;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadResponseDTO {

    private Long id;
    private ResultadoEvaluacion resultado;
    private TipoEvaluacion tipoEvaluacion;
    private LocalDateTime fechaEvaluacion;
    private String observaciones;
    private List<ArchivoEvaluacionDTO> archivosAdjuntos;
    private String nombreLote;         // Referencia al lote evaluado
    private String nombreProducto;     // Nombre del producto evaluado
    private String nombreEvaluador;    // Nombre del usuario evaluador
}
