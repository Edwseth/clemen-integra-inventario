package com.willyes.clemenintegra.bom.dto;

public class DetalleFormulaResponse {
    public Long id;
    public Long formulaId;
    public String formulaNombre;
    public String formulaVersion;
    public String formulaEstado;
    public Long insumoId;
    public String insumoNombre;
    public java.math.BigDecimal cantidadNecesaria;
    public java.math.BigDecimal cantidadTotalNecesaria;
    public java.math.BigDecimal stockDisponible;
    public java.math.BigDecimal stockLibreFefo;
    public java.math.BigDecimal faltanteFefo;
    /**
     * @deprecated usar maximoProducible para semántica explícita.
     */
    public Integer maxProducible;
    public Integer maximoProducible;
    public String estadoStock;
    public String unidad;
    public String unidadSimbolo;
    public String unidadInsumoSimbolo;
    public String unidadInsumoNombre;
    public String unidadInsumoNombrePlural;
    public String unidadProductoFabricableSimbolo;
    public String unidadProductoFabricableNombre;
    public String unidadProductoFabricableNombrePlural;
    public Long unidadMedidaId;
    public Boolean obligatorio;
    // LÍNEA CODEx: nuevos campos para exponer disponibilidad detallada de lotes
    public DisponibilidadInsumoDTO disponibilidad;
    public BloqueanteDTO bloqueante;
    public java.util.List<LoteResumenDTO> lotes;
}
