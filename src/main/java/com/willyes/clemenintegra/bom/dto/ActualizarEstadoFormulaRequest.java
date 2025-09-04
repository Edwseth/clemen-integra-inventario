package com.willyes.clemenintegra.bom.dto;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEstadoFormulaRequest(
        @NotNull EstadoFormula estado,
        @Size(max = 500) String observacion
) {}
