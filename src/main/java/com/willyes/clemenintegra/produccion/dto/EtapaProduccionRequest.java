package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.*;
public class EtapaProduccionRequest {
    @NotBlank
    public String nombre;

    @NotNull
    @Min(1)
    public Integer secuencia;

    @NotNull
    public Long ordenProduccionId;
}
