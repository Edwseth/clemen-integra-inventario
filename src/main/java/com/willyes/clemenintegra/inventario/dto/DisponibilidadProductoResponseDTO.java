package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadProductoResponseDTO {
    private Long productoId;
    private String nombreProducto;
    private Map<String, BigDecimal> totalesPorEstado;
    private List<LoteDisponibleDTO> lotesDisponiblesFIFO;
    private BigDecimal totalDisponible;
    private String unidadMedida;
}
