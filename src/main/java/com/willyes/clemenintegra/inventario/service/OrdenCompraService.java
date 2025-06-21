package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;

    /**
     * Busca una orden de compra por su ID incluyendo proveedor y detalles.
     * @param id ID de la orden
     * @return Optional con la orden completa, o vacío si no existe
     */
    public Optional<OrdenCompra> buscarPorIdConDetalles(Long id) {
        return ordenCompraRepository.findByIdWithDetalles(id);
    }

    // Métodos adicionales futuros: crear, editar, anular, etc.
}

