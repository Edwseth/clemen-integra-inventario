package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoteTerminadoResumenDTO {
    private Long idLote;
    private String codigoLote;
    private String estadoLote;
    private String almacenNombre;
    private LocalDateTime fechaVencimiento;
    private java.math.BigDecimal stockLote;
}

