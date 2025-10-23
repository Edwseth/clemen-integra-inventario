package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Datos para crear un detalle de orden de compra.
 * El identificador de la orden es asignado por el servidor.
 */
public class OrdenCompraDetalleRequestDTO {
    @NotNull
    private Long productoId;

    @NotNull
    @Digits(integer=10, fraction=3)
    @DecimalMin(value = "0.001")
    private BigDecimal cantidad;

    @NotNull
    @Digits(integer=10, fraction=3)
    @DecimalMin(value = "0.00")
    private BigDecimal valorUnitario;

    @NotNull
    @Digits(integer=5, fraction=2)
    @DecimalMin("0.00")
    private BigDecimal iva;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNecesidad;

}
