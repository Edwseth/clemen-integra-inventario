package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAnalisisMicroRequestDTO {
    @NotNull
    private Long parametroId;
    private String resultado;
    private Boolean cumple;
    private String observaciones;
}

