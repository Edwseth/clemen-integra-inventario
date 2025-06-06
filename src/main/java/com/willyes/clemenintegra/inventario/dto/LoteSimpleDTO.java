package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoteSimpleDTO {
    private Long id;
    private String codigoLote;
    private String estado;
    private LocalDate fechaFabricacion;
    private LocalDate fechaVencimiento;
}

