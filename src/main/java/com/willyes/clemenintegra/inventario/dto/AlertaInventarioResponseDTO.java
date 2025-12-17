package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaInventarioResponseDTO {
    private AlertaInventarioTipo tipo;
    private AlertaInventarioSeveridad severidad;
    private Long productoId;
    private String nombreProducto;
    private String codigoSku;
    private Long almacenId;
    private String nombreAlmacen;
    private Long loteProductoId;
    private String codigoLote;
    private LocalDateTime fechaVencimiento;
    private BigDecimal stockActual;
    private BigDecimal umbral;
    private String mensaje;
}
