package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionFisicaResponseDTO {
    private Long id;
    private Integer almacenId;
    private String codigo;
    private String descripcion;
    private boolean activo;
}
