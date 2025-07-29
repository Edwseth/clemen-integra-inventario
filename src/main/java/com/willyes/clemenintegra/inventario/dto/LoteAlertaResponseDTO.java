package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteAlertaResponseDTO {
    private Long loteId;
    private String codigoLote;
    private LocalDateTime fechaVencimiento;
    private String nombreProducto;
    private String nombreAlmacen;
}

