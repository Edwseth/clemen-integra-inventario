package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.validators.ProduccionEtapasLockValidator;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EtapaProduccionServiceImpl implements EtapaProduccionService {

    private final EtapaProduccionRepository repository;
    private final ProduccionEtapasLockValidator etapasLockValidator;

    public List<EtapaProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<EtapaProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public EtapaProduccion guardar(EtapaProduccion etapa) {
        Long ordenId = etapa != null && etapa.getOrdenProduccion() != null ? etapa.getOrdenProduccion().getId() : null;
        if (etapa != null && etapa.getId() != null) {
            Optional<EtapaProduccion> existente = repository.findById(etapa.getId());
            if (existente.isPresent() && existente.get().getOrdenProduccion() != null) {
                ordenId = existente.get().getOrdenProduccion().getId();
            }
        }
        if (ordenId != null) {
            etapasLockValidator.assertEtapasEditables(ordenId);
        }
        return repository.save(etapa);
    }

    public void eliminar(Long id) {
        EtapaProduccion etapa = repository.findById(id).orElse(null);
        if (etapa != null && etapa.getOrdenProduccion() != null && etapa.getOrdenProduccion().getId() != null) {
            etapasLockValidator.assertEtapasEditables(etapa.getOrdenProduccion().getId());
        }
        repository.deleteById(id);
    }
}
