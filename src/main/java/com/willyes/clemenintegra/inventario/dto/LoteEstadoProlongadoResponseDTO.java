package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteEstadoProlongadoResponseDTO {
    private Long loteId;
    private String codigoLote;
    private String estado;
    private LocalDateTime fechaFabricacion;
    private int diasEnEstado;
    private String nombreProducto;
}

