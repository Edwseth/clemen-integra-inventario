package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ConteoCiclicoResumenResponseDTO {
    Long id;
    Integer almacenId;
    EstadoConteoCiclico estado;
    LocalDateTime fechaCreacion;
    LocalDateTime aplicadoEn;
    Long creadoPorId;
    Long aplicadoPorId;
}
