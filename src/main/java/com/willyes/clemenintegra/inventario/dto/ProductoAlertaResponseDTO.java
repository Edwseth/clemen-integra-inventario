package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoAlertaResponseDTO {
    private Long productoId;
    private String nombreProducto;
    private int stockActual;
    private int stockMinimo;
}
