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
public class LoteProductoRequestDTO {
    private String codigoLote;
    private LocalDate fechaFabricacion;
    private LocalDate fechaVencimiento;
    private BigDecimal stockLote;
    private EstadoLote estado;
    private Double temperaturaAlmacenamiento;
    private LocalDate fechaLiberacion;
    private Long productoId;
    private Long almacenId;
    private Long usuario_liberador_id;
    private Long orden_produccion_id;
    private Long produccion_id;

}

