package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventarioRequestDTO {

    @NotNull(message = "La cantidad no puede ser nula")
    //@DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
    private BigDecimal cantidad;

    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;

    private String observaciones;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El ID del almacén es obligatorio")
    private Long almacenId;
}

