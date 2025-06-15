package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;

import java.util.List;
import java.util.Optional;

public interface EtapaProduccionService {
    List<EtapaProduccion> listarTodas();
    Optional<EtapaProduccion> buscarPorId(Long id);
    EtapaProduccion guardar(EtapaProduccion etapa);
    void eliminar(Long id);
}
