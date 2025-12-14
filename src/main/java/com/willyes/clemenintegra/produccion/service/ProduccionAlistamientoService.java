package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;

public interface ProduccionAlistamientoService {
    AlistamientoOrdenProduccionDTO obtenerAlistamientoPorOrden(Long ordenId);
}
