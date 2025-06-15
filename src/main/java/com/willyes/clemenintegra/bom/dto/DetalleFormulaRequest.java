package com.willyes.clemenintegra.bom.dto;

import jakarta.validation.constraints.*;
public class DetalleFormulaRequest {
    @NotNull
    public Long formulaId;

    @NotNull
    public Long insumoId;

    @NotNull
    @Positive
    public Double cantidadNecesaria;

    @NotNull
    public Long unidadMedidaId;

    @NotNull
    public Boolean obligatorio;
}
