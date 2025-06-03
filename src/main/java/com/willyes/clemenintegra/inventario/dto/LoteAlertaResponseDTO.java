package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteAlertaResponseDTO {
    private Long loteId;
    private String codigoLote;
    private LocalDate fechaVencimiento;
    private String nombreProducto;
    private String nombreAlmacen;
}

