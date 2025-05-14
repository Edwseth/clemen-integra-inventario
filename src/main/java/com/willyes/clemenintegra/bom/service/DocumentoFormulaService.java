package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.repository.DocumentoFormulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentoFormulaService {

    @Autowired
    private DocumentoFormulaRepository documentoRepository;

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
