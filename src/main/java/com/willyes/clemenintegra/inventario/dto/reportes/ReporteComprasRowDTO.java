package com.willyes.clemenintegra.inventario.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteComprasRowDTO {

    private String ocCodigo;
    private String estado;
    private String productoCodigo;
    private String productoNombre;
    private String udm;
    private BigDecimal cantidad;
    private LocalDateTime fechaOc;
    private LocalDate fechaPactada;
    private LocalDate fechaRecepcion;
    private String proveedorNombre;
    private String condicionesPago;
    private BigDecimal precioUnitario;
    private BigDecimal iva;
}
