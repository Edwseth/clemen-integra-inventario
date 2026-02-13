package com.willyes.clemenintegra.inventario.regularizacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegularizacionTrazabilidadRequestDTO(
        @NotNull Long ordenProduccionId,
        @NotNull Long productoId,
        @NotNull Long motivoMovimientoId,
        @NotNull Long tipoMovimientoDetalleId,
        String docReferencia,
        @NotBlank @Size(min = 15, max = 500) String observaciones,
        String soporteId,
        Boolean permitirStockNegativo,
        @NotEmpty List<@Valid AjusteLoteDTO> ajustes
) {
}
