package com.willyes.clemenintegra.planeacion.dto;

import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanProduccionResumenDTO {
    private Long id;
    private LocalDate semanaInicio;
    private LocalDate semanaFin;
    private EstadoPlanProduccion estado;
    private String creadoPorNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaConfirmacion;
}
