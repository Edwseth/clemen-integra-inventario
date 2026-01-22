package com.willyes.clemenintegra.bom.dto;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import java.time.LocalDateTime;

public class FormulaProductoSelectorDTO {
    public Long formulaId;
    public String version;
    public String estado;
    public Long productoId;
    public String productoSku;
    public String productoNombre;
    public boolean activo;
    public LocalDateTime fechaActualizacion;
    public String actualizadoPorNombre;
    public LocalDateTime fechaCreacion;
    public String creadoPorNombre;

    public FormulaProductoSelectorDTO(Long formulaId,
                                      String version,
                                      EstadoFormula estado,
                                      Long productoId,
                                      String productoSku,
                                      String productoNombre) {
        this.formulaId = formulaId;
        this.version = version;
        this.estado = estado != null ? estado.name() : null;
        this.productoId = productoId;
        this.productoSku = productoSku;
        this.productoNombre = productoNombre;
    }

    public FormulaProductoSelectorDTO(Long formulaId,
                                      String version,
                                      EstadoFormula estado,
                                      Long productoId,
                                      String productoSku,
                                      String productoNombre,
                                      boolean activo,
                                      LocalDateTime fechaActualizacion,
                                      String actualizadoPorNombre,
                                      LocalDateTime fechaCreacion,
                                      String creadoPorNombre) {
        this.formulaId = formulaId;
        this.version = version;
        this.estado = estado != null ? estado.name() : null;
        this.productoId = productoId;
        this.productoSku = productoSku;
        this.productoNombre = productoNombre;
        this.activo = activo;
        this.fechaActualizacion = fechaActualizacion;
        this.actualizadoPorNombre = actualizadoPorNombre;
        this.fechaCreacion = fechaCreacion;
        this.creadoPorNombre = creadoPorNombre;
    }

    public FormulaProductoSelectorDTO(Long formulaId,
                                      String version,
                                      EstadoFormula estado,
                                      Integer productoId,
                                      String productoSku,
                                      String productoNombre) {
        this(formulaId,
                version,
                estado,
                productoId != null ? productoId.longValue() : null,
                productoSku,
                productoNombre);
    }
}
