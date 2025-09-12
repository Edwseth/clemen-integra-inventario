package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final HistorialEstadoOrdenRepository historialEstadoOrdenRepository;
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

    public Page<OrdenCompraResponseDTO> listarPorEstado(EstadoOrdenCompra estado, Pageable pageable) {
        return ordenCompraRepository.findByEstado(estado, pageable)
                .map(mapper::toDTO);
    }

    public String generarCodigoOrdenCompra() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OC-CLEMEN-" + fecha;
        Long contador = ordenCompraRepository.countByCodigoOrdenStartingWith(prefijo);
        return prefijo + "-" + String.format("%02d", contador + 1);
    }

    /**
     * Evalúa el estado de una orden según la cantidad recibida en sus detalles
     * y actualiza la entidad si corresponde.
     * @param orden Orden de compra a evaluar
     */
    public void evaluarYActualizarEstado(OrdenCompra orden) {
        boolean allReceived = orden.getDetalles().stream()
                .allMatch(d -> d.getCantidadRecibida().compareTo(d.getCantidad()) >= 0);
        boolean anyReceived = orden.getDetalles().stream()
                .anyMatch(d -> d.getCantidadRecibida().compareTo(java.math.BigDecimal.ZERO) > 0);

        EstadoOrdenCompra nuevoEstado = orden.getEstado();
        if (allReceived) {
            nuevoEstado = EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE;
        } else if (anyReceived) {
            nuevoEstado = EstadoOrdenCompra.PARCIALMENTE_RECIBIDA;
        }

        if (nuevoEstado != orden.getEstado()) {
            orden.setEstado(nuevoEstado);
            ordenCompraRepository.save(orden);
        }
    }

    public HistorialEstadoOrden cambiarEstado(Long ordenId, EstadoOrdenCompra estado, Long usuarioId, String observaciones) {
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden de compra no encontrada"));

        orden.setEstado(estado);
        ordenCompraRepository.save(orden);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        HistorialEstadoOrden historial = HistorialEstadoOrden.builder()
                .ordenCompra(orden)
                .estado(estado)
                .fechaCambio(LocalDateTime.now())
                .cambiadoPor(usuario)
                .observaciones(observaciones)
                .build();

        return historialEstadoOrdenRepository.save(historial);
    }

    // Métodos adicionales futuros: crear, editar, anular, etc.
}

