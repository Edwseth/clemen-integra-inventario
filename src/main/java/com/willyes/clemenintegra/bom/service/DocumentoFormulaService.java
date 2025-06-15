package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.repository.DocumentoFormulaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentoFormulaService {

    private final DocumentoFormulaRepository documentoRepository;

    public List<DocumentoFormula> listarTodas() {
        return documentoRepository.findAll();
    }

    public Optional<DocumentoFormula> buscarPorId(Long id) {
        return documentoRepository.findById(id);
    }

    public DocumentoFormula guardar(DocumentoFormula documento) {
        return documentoRepository.save(documento);
    }

    public void eliminar(Long id) {
        documentoRepository.deleteById(id);
    }
}
