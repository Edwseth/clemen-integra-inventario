package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteEstadoProlongadoResponseDTO {
    private Long loteId;
    private String codigoLote;
    private String estado;
    private LocalDate fechaFabricacion;
    private int diasEnEstado;
    private String nombreProducto;
}

