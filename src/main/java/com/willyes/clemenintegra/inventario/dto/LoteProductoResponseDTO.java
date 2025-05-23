package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProductoResponseDTO {
    private Long id;
    private String codigoLote;
    private LocalDate fechaFabricacion;
    private LocalDate fechaVencimiento;
    private BigDecimal stockLote;
    private EstadoLote estado;
    private Double temperaturaAlmacenamiento;
    private LocalDate fechaLiberacion;
    private String nombreProducto;
    private String nombreAlmacen;

}
