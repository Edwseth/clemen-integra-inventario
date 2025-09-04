package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FormulaProductoService {
    List<FormulaProducto> listarTodas();
    Optional<FormulaProducto> buscarPorId(Long id);
    FormulaProducto guardar(FormulaProducto formula);
    void eliminar(Long id);

    FormulaProductoResponse obtenerFormulaActivaPorProducto(Long productoId, BigDecimal cantidad);

    FormulaProductoResponse actualizarEstado(Long id, EstadoFormula nuevoEstado, String observacion, Long usuarioId);
}
