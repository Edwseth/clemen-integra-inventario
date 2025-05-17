package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecificacionCalidadDTO {

    private Long id;

    @NotBlank(message = "El parámetro es obligatorio")
    @Size(max = 45, message = "El parámetro no puede exceder 45 caracteres")
    private String parametro;

    @NotBlank(message = "El valor mínimo es obligatorio")
    @Size(max = 45, message = "El valor mínimo no puede exceder 45 caracteres")
    private String valorMinimo;

    @NotBlank(message = "El valor máximo es obligatorio")
    @Size(max = 45, message = "El valor máximo no puede exceder 45 caracteres")
    private String valorMaximo;

    @NotBlank(message = "El método de ensayo es obligatorio")
    @Size(max = 100, message = "El método de ensayo no puede exceder 100 caracteres")
    private String metodoEnsayo;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;
}

