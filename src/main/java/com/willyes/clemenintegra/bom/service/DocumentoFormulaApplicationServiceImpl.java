package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaRequestDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.DocumentoFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoFormulaApplicationServiceImpl implements DocumentoFormulaApplicationService {

    private final DocumentoFormulaRepository documentoRepository;
    private final FormulaProductoRepository formulaProductoRepository;
    private final BomMapper bomMapper;

    @Override
    public DocumentoFormulaResponseDTO guardar(DocumentoFormulaRequestDTO requestDTO) {
        FormulaProducto formula = formulaProductoRepository.findById(requestDTO.getFormulaId())
                .orElseThrow(() -> new EntityNotFoundException("Fórmula no encontrada"));

        DocumentoFormula entidad = bomMapper.toEntity(requestDTO, formula);
        DocumentoFormula guardado = documentoRepository.save(entidad);
        return bomMapper.toResponseDTO(guardado);
    }

    @Override
    public DocumentoFormulaResponseDTO buscarPorId(Long id) {
        DocumentoFormula encontrado = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));
        return bomMapper.toResponseDTO(encontrado);
    }

    @Override
    public List<DocumentoFormulaResponseDTO> listarTodas() {
        return documentoRepository.findAll()
                .stream()
                .map(entity -> bomMapper.toResponseDTO(entity))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        documentoRepository.deleteById(id);
    }

    @Override
    public DocumentoFormulaResponseDTO actualizar(Long id, DocumentoFormulaRequestDTO requestDTO) {
        DocumentoFormula existente = documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado"));

        FormulaProducto formula = formulaProductoRepository.findById(requestDTO.getFormulaId())
                .orElseThrow(() -> new EntityNotFoundException("Fórmula no encontrada"));

        DocumentoFormula entidadActualizada = bomMapper.toEntity(requestDTO, formula);
        entidadActualizada.setId(id);  // mantenemos el id original

        DocumentoFormula actualizado = documentoRepository.save(entidadActualizada);
        return bomMapper.toResponseDTO(actualizado);
    }
}


