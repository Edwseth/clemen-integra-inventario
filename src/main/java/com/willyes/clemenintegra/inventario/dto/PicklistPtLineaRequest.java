package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PicklistPtLineaRequest(
        @NotNull Long productoId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull PicklistPtModoAsignacion modoAsignacion,
        Long loteProductoId
) {
}
