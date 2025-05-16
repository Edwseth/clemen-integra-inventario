package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventarioDTO {
    private Long id;
    private LocalDateTime fecha;
    private BigDecimal cantidad;
    private String motivo;
    private String observaciones;
    private Long productoId;
    private Long almacenId;
    private Long usuarioId;
}
