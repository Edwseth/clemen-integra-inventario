package com.willyes.clemenintegra.bom.dto;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;

import java.time.LocalDateTime;
import java.util.List;

public class FormulaActivaProduccionDTO {
    public Long formulaId;
    public Long productoId;
    public String codigoProducto;
    public String nombreProducto;
    public String version;
    public EstadoFormula estado;
    public boolean activo;
    public LocalDateTime fechaActualizacion;
    public String usuarioResponsable;
    public List<DetalleFormulaProduccionDTO> detalles;
}
