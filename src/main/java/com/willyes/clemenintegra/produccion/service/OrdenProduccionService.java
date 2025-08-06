package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;

import java.util.List;
import java.util.Optional;

public interface OrdenProduccionService {
    ResultadoValidacionOrdenDTO guardarConValidacionStock(OrdenProduccion orden);
    ResultadoValidacionOrdenDTO crearOrden(OrdenProduccionRequestDTO dto);
    List<OrdenProduccion> listarTodas();
    Optional<OrdenProduccion> buscarPorId(Long id);
    void eliminar(Long id);
}
