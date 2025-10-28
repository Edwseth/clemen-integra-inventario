package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class FefoPreviewResponseDTO {

    List<LoteConsumoDTO> consumos;
    BigDecimal totalTomar;
}
