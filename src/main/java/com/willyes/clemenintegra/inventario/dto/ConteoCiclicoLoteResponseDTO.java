package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class ConteoCiclicoLoteResponseDTO {
    Long id;
    String codigoLote;
    BigDecimal stockLote;
    LocalDateTime fechaVencimiento;
    Long ubicacionFisicaId;
    String ubicacionCodigo;
}
