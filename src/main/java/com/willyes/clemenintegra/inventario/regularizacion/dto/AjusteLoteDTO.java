package com.willyes.clemenintegra.inventario.regularizacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AjusteLoteDTO(
        @NotNull Long loteProductoId,
        @NotBlank String tipo,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal cantidad
) {
}
