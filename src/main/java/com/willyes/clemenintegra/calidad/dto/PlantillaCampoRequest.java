package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoCampoPlantilla;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaCampoRequest {
    private String codigo;
    private String label;
    private TipoCampoPlantilla tipoCampo;
    private Boolean requerido;
    private Integer orden;
    private String configJson;
    private Boolean activo;
}
