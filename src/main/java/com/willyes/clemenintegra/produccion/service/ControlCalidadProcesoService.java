package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ControlCalidadProceso;
import com.willyes.clemenintegra.produccion.repository.ControlCalidadProcesoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ControlCalidadProcesoService {

    @Autowired
    private ControlCalidadProcesoRepository repository;

    public List<ControlCalidadProceso> listarTodas() {
        return repository.findAll();
    }

    public Optional<ControlCalidadProceso> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public ControlCalidadProceso guardar(ControlCalidadProceso registro) {
        return repository.save(registro);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

