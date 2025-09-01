package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoValidacionOrdenDTO {
    private boolean esValida;
    private String mensaje;
    private Integer unidadesMaximasProducibles;
    private List<InsumoFaltanteDTO> insumosFaltantes;
    private OrdenProduccionResponseDTO orden;
    private BigDecimal unidadesProducidas;
}
