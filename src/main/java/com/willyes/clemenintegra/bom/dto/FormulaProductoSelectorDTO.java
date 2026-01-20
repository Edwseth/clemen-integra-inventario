package com.willyes.clemenintegra.bom.dto;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;

public class FormulaProductoSelectorDTO {
    public Long formulaId;
    public String version;
    public String estado;
    public Long productoId;
    public String productoSku;
    public String productoNombre;

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
}
