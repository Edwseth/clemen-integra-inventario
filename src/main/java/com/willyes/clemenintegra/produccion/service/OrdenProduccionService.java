package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrdenProduccionService {

    @Autowired
    private OrdenProduccionRepository repository;

    public List<OrdenProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<OrdenProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public OrdenProduccion guardar(OrdenProduccion orden) {
        return repository.save(orden);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
