package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;

import java.util.List;
import java.util.Optional;

public interface DetalleEtapaService {
    List<DetalleEtapa> listarTodas();
    Optional<DetalleEtapa> buscarPorId(Long id);
    DetalleEtapa guardar(DetalleEtapa detalle);
    void eliminar(Long id);
}
