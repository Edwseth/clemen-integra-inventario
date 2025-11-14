package com.willyes.clemenintegra.bom.dto;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoFormulaRequest(@NotNull EstadoFormula nuevoEstado) {
}
