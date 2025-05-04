package com.willyes.clemenintegra.inventario.application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedidaResponseDTO {
    private Long id;
    private String nombre;
    private String simbolo;
}
