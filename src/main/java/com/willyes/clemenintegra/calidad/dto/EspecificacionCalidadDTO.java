package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecificacionCalidadDTO {

    private Long id;

    @NotBlank(message = "El parámetro es obligatorio")
    @Size(max = 45, message = "El parámetro no puede exceder 45 caracteres")
    private String parametro;

    @NotNull(message = "El valor mínimo es obligatorio")
    private BigDecimal valorMinimo;

    @NotNull(message = "El valor máximo es obligatorio")
    private BigDecimal valorMaximo;

    @NotBlank(message = "La unidad es obligatoria")
    @Size(max = 45, message = "La unidad no puede exceder 45 caracteres")
    private String unidad;

    @NotBlank(message = "El método de ensayo es obligatorio")
    @Size(max = 100, message = "El método de ensayo no puede exceder 100 caracteres")
    private String metodoEnsayo;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;
}

