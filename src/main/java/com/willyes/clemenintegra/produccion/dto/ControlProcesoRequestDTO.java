package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ControlProcesoRequestDTO {
    @NotBlank
    public String etapa;

    @NotBlank
    public String parametro;

    @NotBlank
    public String valorMedido;

    @NotBlank
    public String unidad;

    @NotNull
    public Boolean cumple;

    public String observaciones;
}
