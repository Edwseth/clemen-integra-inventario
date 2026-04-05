package com.willyes.clemenintegra.produccion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticoInsumosOrdenDTO {
    private Long productoId;
    private Long formulaId;
    private boolean disponibilidadSuficiente;
    private Integer unidadesMaximasProducibles;
    private String unidadProductoFabricableSimbolo;
    private String unidadProductoFabricableNombre;
    private String unidadProductoFabricableNombrePlural;
    private List<DetalleDiagnosticoInsumoDTO> detalleInsumos;
    private List<InsumoFaltanteDTO> insumosFaltantes;
}
