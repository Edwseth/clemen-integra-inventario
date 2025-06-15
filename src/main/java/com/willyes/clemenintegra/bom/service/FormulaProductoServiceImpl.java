package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.repository.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FormulaProductoServiceImpl implements FormulaProductoService {

    private final FormulaProductoRepository formulaRepository;

    public List<FormulaProducto> listarTodas() {
        return formulaRepository.findAll();
    }

    public Optional<FormulaProducto> buscarPorId(Long id) {
        return formulaRepository.findById(id);
    }

    public FormulaProducto guardar(FormulaProducto formula) {
        return formulaRepository.save(formula);
    }

    public void eliminar(Long id) {
        formulaRepository.deleteById(id);
    }
}

