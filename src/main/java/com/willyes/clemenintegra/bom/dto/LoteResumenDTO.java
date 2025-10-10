package com.willyes.clemenintegra.bom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LoteResumenDTO {
    private Long id;
    private String codigoLote;
    private EstadoLote estado;
    private String almacenNombre;
    private BigDecimal stockDisponible;
    private LocalDateTime fechaVencimiento;
    private LocalDateTime fechaLiberacion;
    private String nombreUsuarioLiberador;

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
