package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ObservacionProcesoRequestDTO {
    @NotBlank
    public String tipo;

    @NotBlank
    public String descripcion;
}
