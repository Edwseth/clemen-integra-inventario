package com.willyes.clemenintegra.calidad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
