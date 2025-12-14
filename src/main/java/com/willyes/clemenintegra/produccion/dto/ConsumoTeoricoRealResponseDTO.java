package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ConsumoTeoricoRealResponseDTO {

    private Long ordenId;
    private String codigoOrden;
    private String productoTerminadoSku;
    private String productoTerminadoNombre;
    private BigDecimal cantidadProgramada;
    private BigDecimal cantidadProducida;

    private List<ConsumoTeoricoRealItemDTO> items;
    private List<LoteTerminadoResumenDTO> lotesTerminados;
}

