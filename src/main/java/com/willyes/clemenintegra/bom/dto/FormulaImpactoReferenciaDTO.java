package com.willyes.clemenintegra.bom.dto;

public class FormulaImpactoReferenciaDTO {
    public Long formulaId;
    public String formulaVersion;
    public String formulaEstado;
    public Boolean formulaActivo;
    public Long productoId;
    public String productoSku;
    public String productoNombre;

    public FormulaImpactoReferenciaDTO(Long formulaId,
                                       String formulaVersion,
                                       String formulaEstado,
                                       Boolean formulaActivo,
                                       Integer productoId,
                                       String productoSku,
                                       String productoNombre) {
        this.formulaId = formulaId;
        this.formulaVersion = formulaVersion;
        this.formulaEstado = formulaEstado;
        this.formulaActivo = formulaActivo;
        this.productoId = productoId != null ? productoId.longValue() : null;
        this.productoSku = productoSku;
        this.productoNombre = productoNombre;
    }
}
