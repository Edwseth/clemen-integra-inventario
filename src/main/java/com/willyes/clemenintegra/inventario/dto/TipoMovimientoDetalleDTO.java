package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoMovimientoDetalleDTO {
    private Long id;
    private String descripcion;
}
