package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EtapaProduccionServiceImpl implements EtapaProduccionService {

    private final EtapaProduccionRepository repository;

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
