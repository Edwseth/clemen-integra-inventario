package com.willyes.clemenintegra.bom.dto;

public class DetalleFormulaResponse {
    public Long id;
    public Long formulaId;
    public String formulaNombre;
    public String formulaVersion;
    public String formulaEstado;
    public String insumoNombre;
    public java.math.BigDecimal cantidadNecesaria;
    public java.math.BigDecimal cantidadTotalNecesaria;
    public java.math.BigDecimal stockDisponible;
    public java.math.BigDecimal stockLibreFefo;
    public java.math.BigDecimal faltanteFefo;
    public Integer maxProducible;
    public String estadoStock;
    public String unidad;
    public String unidadSimbolo;
    public Long unidadMedidaId;
    public Boolean obligatorio;
    // LÍNEA CODEx: nuevos campos para exponer disponibilidad detallada de lotes
    public DisponibilidadInsumoDTO disponibilidad;
    public BloqueanteDTO bloqueante;
    public java.util.List<LoteResumenDTO> lotes;
}
