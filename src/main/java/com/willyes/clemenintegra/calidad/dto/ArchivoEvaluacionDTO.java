package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoEvaluacionDTO {
    private String nombreArchivo;
    private String nombreVisible;
}
