package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroAnalisisMicroDTO {
    private Long id;
    private String nombreEnsayo;
    private String metodo;
    private String unidad;
    private String especificacion;
    private TipoResultadoAnalisis tipoResultado;
    private Integer orden;
}

