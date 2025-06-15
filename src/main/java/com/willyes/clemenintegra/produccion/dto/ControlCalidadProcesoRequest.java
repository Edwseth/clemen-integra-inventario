package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.*;
public class ControlCalidadProcesoRequest {
    @NotBlank
    public String parametro;

    @NotBlank
    public String valorMedido;

    @NotNull
    public Boolean cumple;

    @Size(max = 255)
    public String observaciones;

    @NotNull
    public Long detalleEtapaId;

    @NotNull
    public Long evaluadorId;
}
