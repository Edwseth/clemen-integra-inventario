package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.Produccion;
import com.willyes.clemenintegra.produccion.repository.ProduccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduccionService {

    @Autowired
    private ProduccionRepository repository;

    public List<Produccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<Produccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Produccion guardar(Produccion produccion) {
        return repository.save(produccion);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

