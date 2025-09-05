package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenProduccionRequestDTO {
    @NotNull
    @PastOrPresent
    private LocalDateTime fechaInicio;

    @NotNull
    @FutureOrPresent
    private LocalDateTime fechaFin;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cantidadProgramada;

    @DecimalMin(value = "0.0")
    private BigDecimal cantidadProducida;

    @NotBlank
    private String estado;

    @NotNull
    private Long productoId;

    @NotNull
    private Long responsableId;

    /**
     * Símbolo de la unidad de medida en la que se expresa la cantidad programada
     * (por ejemplo, "kg" o "L"). Si se omite, se utilizará la unidad definida
     * para el producto.
     */
    @NotBlank
    private String unidadMedidaSimbolo;
}
