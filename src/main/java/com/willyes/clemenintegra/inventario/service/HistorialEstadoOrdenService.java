package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;

import java.util.List;

public interface HistorialEstadoOrdenService {
    List<HistorialEstadoOrden> listarTodos();
    List<HistorialEstadoOrden> listarPorOrden(Long ordenId);
    HistorialEstadoOrden guardar(HistorialEstadoOrden historial);
}
