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
public class LoteDisponibleDTO {
    private Long loteProductoId;
    private String codigoLote;
    private BigDecimal stockLote;
    private LocalDateTime fechaVencimiento;
    private Integer almacenId;
    private String nombreAlmacen;
}
