package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CondicionUsoResponseDTO {

    private Long id;
    private Long loteId;
    private TipoCondicionUso tipo;
    private LocalDateTime parametroFecha;
    private String descripcion;
    private EstadoCondicionUso estado;
    private LocalDateTime creadoEn;
    private Long creadoPor;
}
