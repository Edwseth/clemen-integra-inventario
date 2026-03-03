package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaAnalisisResumenDTO {
    private Long id;
    private Long productoId;
    private String nombre;
    private TipoAnalisisPlantilla tipoAnalisis;
    private Integer version;
    private boolean vigente;
    private boolean activo;
    private Integer cantidadCampos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
