package com.willyes.clemenintegra.bom.dto;

public class DetalleFormulaResponse {
    public Long id;
    public String insumoNombre;
    public java.math.BigDecimal cantidadNecesaria;
    public java.math.BigDecimal cantidadTotalNecesaria;
    public java.math.BigDecimal stockActual;
    public String estadoStock;
    public String unidad;
    public Boolean obligatorio;
    // LÍNEA CODEx: nuevos campos para exponer disponibilidad detallada de lotes
    public DisponibilidadInsumoDTO disponibilidad;
    public BloqueanteDTO bloqueante;
    public java.util.List<LoteResumenDTO> lotes;
}
