package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.FormulaProducto;

import java.util.List;
import java.util.Optional;

public interface FormulaProductoService {
    List<FormulaProducto> listarTodas();
    Optional<FormulaProducto> buscarPorId(Long id);
    FormulaProducto guardar(FormulaProducto formula);
    void eliminar(Long id);
}
