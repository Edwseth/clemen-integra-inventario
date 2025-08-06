package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsumoFaltanteDTO {
    private Long productoId;
    private String nombre;
    private BigDecimal requerido;
    private BigDecimal disponible;
}

