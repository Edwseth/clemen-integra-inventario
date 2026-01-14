package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PicklistPtCreateRequest(
        @NotBlank String clienteNombre,
        Integer minVidaUtilDias,
        String docReferencia,
        String observaciones,
        @NotEmpty List<PicklistPtLineaRequest> lineas
) {
}
