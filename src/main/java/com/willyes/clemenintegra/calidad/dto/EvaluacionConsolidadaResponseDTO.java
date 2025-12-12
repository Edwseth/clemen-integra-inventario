package com.willyes.clemenintegra.calidad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionConsolidadaResponseDTO {
    private Long id;
    private String nombreLote;
    private String nombreProducto;
    private String estadoLote;
    private String tipoAnalisisCalidad;
    private String tipoAnalisisRequerido;
    private boolean fisicoQuimicoCargado;
    private boolean microbiologicoCargado;
    private boolean evaluacionesRequeridasCompletas;
    private boolean requiereAnalisisFisico;
    private boolean requiereAnalisisQuimico;
    private boolean requiereAnalisisMicrobiologico;
    private boolean tieneEvaluacionFisica;
    private boolean tieneEvaluacionQuimicaMicro;
    private boolean tieneResultadosMicro;
    private boolean tieneAdjuntosFisico;
    private boolean tieneAdjuntosQuimicoMicro;
    private Long evaluacionQuimicoMicroId;
    private Long evaluacionFisicaId;
    private EstadoEvaluacionCalidad estadoEvaluacion;
    private List<ArchivoEvaluacionDTO> adjuntosQuimicoMicro;
    private String resultadoGlobal;
    private List<EvaluacionSimpleDTO> evaluaciones;

    /**
     * @deprecated Usar {@link #getId()}. Este alias se retirará el 31/12/2024.
     */
    @Deprecated(since = "2024-09-01", forRemoval = true)
    @JsonProperty("idLote")
    public Long getIdLote() {
        return id;
    }

    /**
     * @deprecated Usar {@link #setId(Long)}. Este alias se retirará el 31/12/2024.
     */
    @Deprecated(since = "2024-09-01", forRemoval = true)
    @JsonProperty(value = "idLote", access = JsonProperty.Access.WRITE_ONLY)
    public void setIdLote(Long legacyId) {
        this.id = legacyId;
    }
}
