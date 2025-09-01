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
    @NotBlank
    private String loteProduccion;

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

    private String unidadMedidaSimbolo;
}
