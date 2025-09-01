package com.willyes.clemenintegra.produccion.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinalizarOrdenRequestDTO {
    private BigDecimal cantidadProducida;
}
