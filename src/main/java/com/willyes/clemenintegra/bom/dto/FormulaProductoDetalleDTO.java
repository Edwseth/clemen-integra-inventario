package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FormulaProductoDetalleDTO {
    public Long id;
    public String codigo;
    public String version;
    public String estado;
    public Long productoId;
    public String productoNombre;
    public LocalDateTime fechaCreacion;
    public String creadoPorNombre;
    public String observacion;
    public LocalDateTime fechaActualizacion;
    public String actualizadoPorNombre;
    public List<DetalleFormulaDetalleDTO> detalles;
    public List<DocumentoFormulaResponseDTO> documentos;
}
