package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspecificacionFisicoQuimicaDTO {
    private Long id;
    private Long productoId;
    private String codigoSku;
    private String nombreProducto;
    private String nombreParametro;
    private String unidad;
    private BigDecimal limiteInferior;
    private BigDecimal limiteSuperior;
    private String observaciones;
    private boolean activo;
    private String creadoPorNombre;
    private LocalDateTime fechaCreacion;
}
