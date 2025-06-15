package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.Produccion;

import java.util.List;
import java.util.Optional;

public interface ProduccionService {
    List<Produccion> listarTodas();
    Optional<Produccion> buscarPorId(Long id);
    Produccion guardar(Produccion produccion);
    void eliminar(Long id);
}
