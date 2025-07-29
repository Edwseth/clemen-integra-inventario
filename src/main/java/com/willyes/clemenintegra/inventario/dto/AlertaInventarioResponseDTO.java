package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaInventarioResponseDTO {
    private String tipo;
    private String nombreProducto;
    private String nombreAlmacen;
    private String codigoLote;
    private LocalDateTime fechaVencimiento;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private String estado;
    private String criticidad;
}
