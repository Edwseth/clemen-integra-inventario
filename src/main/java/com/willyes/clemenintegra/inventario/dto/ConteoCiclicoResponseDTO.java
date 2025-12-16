package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ConteoCiclicoResponseDTO {
    Long id;
    Integer almacenId;
    EstadoConteoCiclico estado;
    LocalDateTime fechaCreacion;
    LocalDateTime aplicadoEn;
    Long creadoPorId;
    Long aplicadoPorId;
    List<ConteoCiclicoDetalleResponseDTO> detalles;
}
