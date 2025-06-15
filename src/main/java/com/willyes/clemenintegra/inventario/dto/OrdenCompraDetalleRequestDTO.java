package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDetalleRequestDTO {
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal cantidad;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal valorUnitario;

    private BigDecimal iva;

    @NotNull
    private Long productoId;

    @NotNull
    private Long ordenCompraId;

}
