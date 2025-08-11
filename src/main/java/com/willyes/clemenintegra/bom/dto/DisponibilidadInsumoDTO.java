package com.willyes.clemenintegra.bom.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DisponibilidadInsumoDTO {
    private BigDecimal disponible = BigDecimal.ZERO;
    private BigDecimal enCuarentena = BigDecimal.ZERO;
    private BigDecimal retenido = BigDecimal.ZERO;
    private BigDecimal rechazado = BigDecimal.ZERO;
    private BigDecimal vencido = BigDecimal.ZERO;
    private BigDecimal totalProducto = BigDecimal.ZERO;
}
