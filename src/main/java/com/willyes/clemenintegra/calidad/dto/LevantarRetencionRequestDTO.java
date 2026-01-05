package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevantarRetencionRequestDTO {
    @NotBlank(message = "La observación es obligatoria")
    private String observacion;
}

