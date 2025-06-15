package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaRequestDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;

import java.util.List;
import java.util.Optional;

public interface DocumentoFormulaApplicationService {
    DocumentoFormulaResponseDTO guardar(DocumentoFormulaRequestDTO requestDTO);
    DocumentoFormulaResponseDTO buscarPorId(Long id);
    List<DocumentoFormulaResponseDTO> listarTodas();
    void eliminar(Long id);
    DocumentoFormulaResponseDTO actualizar(Long id, DocumentoFormulaRequestDTO request);
}
