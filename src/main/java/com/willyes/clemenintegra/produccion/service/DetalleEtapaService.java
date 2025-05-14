package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.repository.DetalleEtapaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetalleEtapaService {

    @Autowired
    private DetalleEtapaRepository repository;

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

