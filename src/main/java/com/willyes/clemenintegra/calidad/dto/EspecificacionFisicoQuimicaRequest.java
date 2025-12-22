package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecificacionFisicoQuimicaRequest {
    private String nombreParametro;
    private String unidad;
    private BigDecimal limiteInferior;
    private BigDecimal limiteSuperior;
    private String observaciones;
    private Boolean activo;
}
