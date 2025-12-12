package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAnalisisMicroDetalleDTO {
    private Long parametroId;
    private String nombreEnsayo;
    private String especificacion;
    private String unidad;
    private String resultado;
    private Boolean cumple;
    private String observaciones;
}
