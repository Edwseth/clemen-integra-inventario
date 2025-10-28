package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class LoteConsumoDTO {

    Long loteId;
    String codigoLote;
    LocalDateTime fechaVencimiento;
    Long almacenId;
    BigDecimal disponibleAntes;
    BigDecimal tomar;
    BigDecimal disponibleDespues;
}
