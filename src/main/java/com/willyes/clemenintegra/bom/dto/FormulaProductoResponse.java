package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FormulaProductoResponse {
    public Long id;
    public String productoNombre;
    public String version;
    public String estado;
    public LocalDateTime fechaCreacion;
    public String creadoPorNombre;
    public List<DetalleFormulaResponse> detalles;
    public List<DocumentoFormulaResponse> documentos;
}
