package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorialEstadoOrdenService {

    private final HistorialEstadoOrdenRepository repository;

    public List<HistorialEstadoOrden> listarTodos() {
        return repository.findAll();
    }

    public HistorialEstadoOrden guardar(HistorialEstadoOrden historial) {
        return repository.save(historial);
    }
}

