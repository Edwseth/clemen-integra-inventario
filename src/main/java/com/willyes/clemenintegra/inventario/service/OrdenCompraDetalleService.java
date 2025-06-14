package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrdenCompraDetalleService {

    @Autowired
    private OrdenCompraDetalleRepository repository;

    public List<OrdenCompraDetalle> listarTodos() {
        return repository.findAll();
    }

    public Optional<OrdenCompraDetalle> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public OrdenCompraDetalle guardar(OrdenCompraDetalle detalle) {
        return repository.save(detalle);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

