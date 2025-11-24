package com.willyes.clemenintegra.planeacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaAbastecimientoResponseDTO {
    private Long id;
    private Long corridaId;
    private Long productoId;
    private String productoSku;
    private String productoNombre;
    private String categoriaProducto;
    private String tipoSugerencia;
    private BigDecimal cantidadSugerida;
    private LocalDate fechaRequerida;
    private LocalDate fechaSugeridaPedido;
    private String estado;
    private String origen;
    private String observaciones;
}
