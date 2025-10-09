package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoConformidadDTO {

    private Long id;

    @NotNull(message = "El código es obligatorio")
    @Size(max = 100, message = "El código no puede exceder 100 caracteres")
    private String codigo;

    @NotNull(message = "El origen es obligatorio")
    private OrigenNoConformidad origen;

    @NotNull(message = "La severidad es obligatoria")
    private SeveridadNoConformidad severidad;

    private EstadoNoConformidad estado;

    private String descripcion;

    @Size(max = 255, message = "La evidencia no puede exceder 255 caracteres")
    private String evidencia;

    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDateTime fechaRegistro;

    @NotNull(message = "El usuario que reporta es obligatorio")
    private Long usuarioReportaId;

    private Long loteId;

    private Long productoId;

    private Long evaluacionId;

    private Long creadoPor;

    private Long actualizadoPor;

    private LocalDateTime actualizadoEn;
}

