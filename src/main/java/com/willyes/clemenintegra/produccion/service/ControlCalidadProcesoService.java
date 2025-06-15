package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.ControlCalidadProceso;

import java.util.List;
import java.util.Optional;

public interface ControlCalidadProcesoService {
    List<ControlCalidadProceso> listarTodas();
    Optional<ControlCalidadProceso> buscarPorId(Long id);
    ControlCalidadProceso guardar(ControlCalidadProceso registro);
    void eliminar(Long id);
}
