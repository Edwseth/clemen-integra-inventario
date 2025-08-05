package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.*;

/**
 * Representa la información necesaria para registrar una fórmula de producto
 * junto con sus insumos asociados.
 */
public class FormulaProductoRequest {
    @NotNull
    private Long productoId;

    @NotBlank
    private String version;

    @NotBlank
    private String estado;

    @PastOrPresent
    private LocalDateTime fechaCreacion;

    @NotNull
    private Long creadoPorId;

    /**
     * Lista de insumos que componen la fórmula. El nombre del campo coincide con
     * el JSON recibido para permitir la deserialización automática.
     */
    private List<InsumoRequestDTO> insumos;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getCreadoPorId() {
        return creadoPorId;
    }

    public void setCreadoPorId(Long creadoPorId) {
        this.creadoPorId = creadoPorId;
    }

    public List<InsumoRequestDTO> getInsumos() {
        return insumos;
    }

    public void setInsumos(List<InsumoRequestDTO> insumos) {
        this.insumos = insumos;
    }

}
