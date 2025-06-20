package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoMovimientoDetalleResponseDTO {
    private Long id;
    private String descripcion;
}

