package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;

/**
 * DTO de resumen utilizado para el listado maestro de fórmulas BOM.
 */
public class FormulaProductoResumenDTO {
    public Long id;
    public Long productoId;
    public String codigoProducto;
    public String nombreProducto;
    public String version;
    public String estado;
    public boolean activo;
    public LocalDateTime fechaActualizacion;
    public String usuarioResponsable;
}

