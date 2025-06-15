package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraDetalleService {
    List<OrdenCompraDetalle> listarTodos();
    Optional<OrdenCompraDetalle> buscarPorId(Long id);
    OrdenCompraDetalle guardar(OrdenCompraDetalle detalle);
    void eliminar(Long id);
}
