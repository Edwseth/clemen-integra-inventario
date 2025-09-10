package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoAlertaResponseDTO {
    private Long productoId;
    private String nombreProducto;
    private BigDecimal stockDisponible;
    private BigDecimal stockMinimo;
}
