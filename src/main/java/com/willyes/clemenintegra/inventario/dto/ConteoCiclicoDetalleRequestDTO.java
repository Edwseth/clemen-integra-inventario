package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConteoCiclicoDetalleRequestDTO {

    @NotNull
    private Long productoId;
    private Long loteProductoId;
    private Long ubicacionFisicaId;

    private BigDecimal stockSistema;

    @NotNull
    @DecimalMin(value = "0.00", message = "El conteo físico no puede ser negativo")
    private BigDecimal conteoFisico;
}
