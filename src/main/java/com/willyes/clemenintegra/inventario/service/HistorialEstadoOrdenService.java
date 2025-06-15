package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;

import java.util.List;

public interface HistorialEstadoOrdenService {
    List<HistorialEstadoOrden> listarTodos();
    HistorialEstadoOrden guardar(HistorialEstadoOrden historial);
}
