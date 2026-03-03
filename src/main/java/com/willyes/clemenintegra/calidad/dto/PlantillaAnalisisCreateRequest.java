package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaAnalisisCreateRequest {
    private String nombre;
    private Boolean vigente;
    private List<PlantillaCampoRequest> campos;
}
