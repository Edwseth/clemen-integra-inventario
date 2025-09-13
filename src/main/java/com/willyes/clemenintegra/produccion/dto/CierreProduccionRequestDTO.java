package com.willyes.clemenintegra.produccion.dto;

import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreProduccionRequestDTO {
    @NotNull
    private BigDecimal cantidad;
    @NotNull
    private TipoCierre tipo;
    private String codigoLote;
    private Boolean cerradaIncompleta;
    private String turno;
    private String observacion;
}
