package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionConsolidadaResponseDTO {
    private Long idLote;
    private String nombreLote;
    private String nombreProducto;
    private String estadoLote;
    private String tipoAnalisisCalidad;
    private List<EvaluacionSimpleDTO> evaluaciones;
}
