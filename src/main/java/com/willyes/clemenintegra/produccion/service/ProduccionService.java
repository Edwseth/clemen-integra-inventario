package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.Produccion;
import com.willyes.clemenintegra.produccion.repository.ProduccionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProduccionService {

    private final ProduccionRepository repository;

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

