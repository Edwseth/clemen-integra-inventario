package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventarioResponseDTO {
    private Long id;
    private LocalDateTime fecha;
    private BigDecimal cantidad;
    private String motivo;
    private String observaciones;
    private String productoNombre;
    private String almacenNombre;
    private String usuarioNombre;
}

