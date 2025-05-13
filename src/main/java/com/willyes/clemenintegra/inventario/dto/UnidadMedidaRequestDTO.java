package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedidaRequestDTO {
    @NotBlank
    @Size(max = 50)
    private String nombre;

    @NotBlank
    @Size(max = 5)
    private String simbolo;
}

