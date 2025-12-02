package com.willyes.clemenintegra.bom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO que representa cada insumo incluido en la creación de una fórmula.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsumoRequestDTO {

    @NotNull
    private Long productoId;

    @NotNull
    @Positive
    private Double cantidad;

    @NotBlank
    private String unidadMedida;

    @NotBlank
    private String tipo;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
