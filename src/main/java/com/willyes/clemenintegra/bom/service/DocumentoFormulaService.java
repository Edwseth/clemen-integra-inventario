package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DocumentoFormula;

import java.util.List;
import java.util.Optional;

public interface DocumentoFormulaService {
    List<DocumentoFormula> listarTodas();
    Optional<DocumentoFormula> buscarPorId(Long id);
    DocumentoFormula guardar(DocumentoFormula documento);
    void eliminar(Long id);
}
