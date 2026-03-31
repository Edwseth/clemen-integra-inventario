package com.willyes.clemenintegra.inventario.dto.valorizacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AjusteValorizacionLoteRequestDTO(
        @NotNull(message = "El costo unitario nuevo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El costo unitario nuevo debe ser mayor a cero")
        BigDecimal costoUnitarioNuevo,

        @NotBlank(message = "El motivo es obligatorio")
        String motivo,

        @NotBlank(message = "La observación es obligatoria")
        String observacion,

        @NotBlank(message = "El documento de soporte es obligatorio")
        String documentoSoporte,

        Boolean forzarSobreCostoPositivo
) {
}
