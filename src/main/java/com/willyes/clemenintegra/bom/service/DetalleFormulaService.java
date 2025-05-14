package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetalleFormulaService {

    @Autowired
    private DetalleFormulaRepository detalleRepository;

    public List<DetalleFormula> listarTodas() {
        return detalleRepository.findAll();
    }

    public Optional<DetalleFormula> buscarPorId(Long id) {
        return detalleRepository.findById(id);
    }

    public DetalleFormula guardar(DetalleFormula detalle) {
        return detalleRepository.save(detalle);
    }

    public void eliminar(Long id) {
        detalleRepository.deleteById(id);
    }
}
