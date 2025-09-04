package com.willyes.clemenintegra.bom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class FormulaProductoResponse {
    public Long id;
    public String productoNombre;
    public String version;
    public String estado;
    public LocalDateTime fechaCreacion;
    public String creadoPorNombre;
    public String observacion;
    public LocalDateTime fechaActualizacion;
    public String actualizadoPorNombre;
    public BigDecimal cantidadBaseFormula;
    public String unidadBaseFormula;
    public List<DetalleFormulaResponse> detalles;
    public List<DocumentoFormulaResponseDTO> documentos;
}
