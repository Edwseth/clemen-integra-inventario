package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KardexFiltro {
    private Long productoId;
    private String codigoSku;
    private Long loteId;
    private String codigoLote;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private Long almacenId;
    private Long ordenProduccionId;
    private Long etapaProduccionId;
}
