package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraMapper mapper;

    /**
     * Busca una orden de compra por su ID incluyendo proveedor y detalles.
     * @param id ID de la orden
     * @return Optional con la orden completa, o vacío si no existe
     */
    public Optional<OrdenCompra> buscarPorIdConDetalles(Long id) {
        return ordenCompraRepository.findByIdWithDetalles(id);
    }

    public Page<OrdenCompraResponseDTO> listar(Pageable pageable) {
        return ordenCompraRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    // Métodos adicionales futuros: crear, editar, anular, etc.
}

