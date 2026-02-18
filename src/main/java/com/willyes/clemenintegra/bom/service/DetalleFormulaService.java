package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;

import java.util.List;
import java.util.Optional;

public interface DetalleFormulaService {
    List<DetalleFormula> listarTodas(Long formulaId, String insumo);
    Optional<DetalleFormula> buscarPorId(Long id);
    DetalleFormula guardar(DetalleFormula detalle);
    void eliminar(Long id);
}
