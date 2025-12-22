package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaAnalisisMicrobiologicoResumenDTO {
    private Long id;
    private Long productoId;
    private String codigoSku;
    private String nombreProducto;
    private String nombre;
    private Integer numeroVersion;
    private boolean vigente;
    private LocalDate fechaVigenciaDesde;
    private LocalDate fechaVigenciaHasta;
    private String creadoPorNombre;
    private LocalDateTime fechaCreacion;
    private Integer numeroParametros;
}
