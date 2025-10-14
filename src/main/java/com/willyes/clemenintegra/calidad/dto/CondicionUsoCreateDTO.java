package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CondicionUsoCreateDTO {

    @NotNull
    private Long loteId;

    @NotNull
    private TipoCondicionUso tipo;

    private LocalDateTime parametroFecha;

    private String descripcion;
}
