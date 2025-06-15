package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.repository.DetalleEtapaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetalleEtapaServiceImpl implements DetalleEtapaService {

    private final DetalleEtapaRepository repository;

    public List<DetalleEtapa> listarTodas() {
        return repository.findAll();
    }

    public Optional<DetalleEtapa> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public DetalleEtapa guardar(DetalleEtapa detalle) {
        return repository.save(detalle);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

