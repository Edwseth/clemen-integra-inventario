package com.willyes.clemenintegra.inventario.application.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponseDTO {
    private Long id;
    private String codigoSku;
    private String nombre;
    private String descripcionProducto;
    private Integer stockActual;
    private Integer stockMinimo;
    private Boolean activo;
    private Boolean requiereInspeccion;
    private String unidadMedida;
    private String categoria;
    private LocalDateTime fechaCreacion;
}

