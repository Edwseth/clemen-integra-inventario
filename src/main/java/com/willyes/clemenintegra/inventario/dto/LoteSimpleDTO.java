package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoteSimpleDTO {
    private Long id;
    private String codigoLote;
    private String estado;
    private LocalDateTime fechaFabricacion;
    private LocalDateTime fechaVencimiento;

}

