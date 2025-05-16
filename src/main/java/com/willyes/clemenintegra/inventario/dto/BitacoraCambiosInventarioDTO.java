package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraCambiosInventarioDTO {
    private Long id;
    private String tablaAfectada;
    private Long registroId;
    private String campoModificado;
    private String valorAnt;
    private String valorNuevo;
    private LocalDateTime fechaCambio;
    private Long usuarioId;
}

