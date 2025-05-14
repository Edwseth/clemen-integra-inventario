package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EtapaProduccionService {

    @Autowired
    private EtapaProduccionRepository repository;

    public List<EtapaProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<EtapaProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public EtapaProduccion guardar(EtapaProduccion etapa) {
        return repository.save(etapa);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
