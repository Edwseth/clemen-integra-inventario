package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistorialEstadoOrdenService {

    @Autowired
    private HistorialEstadoOrdenRepository repository;

    public List<HistorialEstadoOrden> listarTodos() {
        return repository.findAll();
    }

    public HistorialEstadoOrden guardar(HistorialEstadoOrden historial) {
        return repository.save(historial);
    }
}

