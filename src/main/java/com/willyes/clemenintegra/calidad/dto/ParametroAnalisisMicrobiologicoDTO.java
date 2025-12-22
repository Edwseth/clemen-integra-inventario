package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroAnalisisMicrobiologicoDTO {
    private Long id;
    private String nombreParametro;
    private String unidad;
    private String criterioAceptacion;
    private TipoResultadoAnalisis tipoResultado;
    private Integer orden;
}
