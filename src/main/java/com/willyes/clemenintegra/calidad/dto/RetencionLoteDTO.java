package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetencionLoteDTO {

    private Long id;
    private String codigoLote;

    @NotNull(message = "El lote es obligatorio")
    private Long loteId;

    @NotNull(message = "La causa es obligatoria")
    @jakarta.validation.constraints.NotBlank(message = "La causa es obligatoria")
    private String causa;

    @NotNull(message = "La fecha de retención es obligatoria")
    private LocalDateTime fechaRetencion;

    private LocalDateTime fechaLiberacion;

    @NotNull(message = "El estado es obligatorio")
    private EstadoRetencion estado;

    @NotNull(message = "El aprobador es obligatorio")
    private Long aprobadoPorId;
    private String aprobadoPorNombre;
    private MotivoRetencion motivo;
    private Long noConformidadId;


}
