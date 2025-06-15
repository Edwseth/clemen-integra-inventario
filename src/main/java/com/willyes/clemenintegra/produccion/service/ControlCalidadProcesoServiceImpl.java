package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ControlCalidadProceso;
import com.willyes.clemenintegra.produccion.repository.ControlCalidadProcesoRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ControlCalidadProcesoServiceImpl implements ControlCalidadProcesoService {

    private final ControlCalidadProcesoRepository repository;

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

