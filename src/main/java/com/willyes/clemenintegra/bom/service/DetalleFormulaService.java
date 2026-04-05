package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DetalleFormulaService {
    Page<DetalleFormula> listarTodas(Long formulaId, String formulaNombre, String insumo, Pageable pageable);
    Optional<DetalleFormula> buscarPorId(Long id);
    DetalleFormula guardar(DetalleFormula detalle);
    void eliminar(Long id);
}
