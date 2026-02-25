package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.RecepcionOCResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.mapper.RecepcionOCMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.RecepcionOCRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.List;

import com.willyes.clemenintegra.shared.model.Usuario;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionOCServiceImpl implements RecepcionOCService {

    private final RecepcionOCRepository recepcionOCRepository;
    private final CodigoRecepcionService codigoRecepcionService;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoInventarioMapper movimientoInventarioMapper;
    private final RecepcionOCMapper recepcionOCMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public RecepcionOC findOrCreateCabecera(Integer ordenCompraId,
                                            Integer almacenDestinoId,
                                            Integer proveedorId,
                                            Long usuarioId,
                                            LocalDate fechaNegocio,
                                            String observaciones) {
        Objects.requireNonNull(ordenCompraId, "ordenCompraId es obligatorio");
        Objects.requireNonNull(almacenDestinoId, "almacenDestinoId es obligatorio");
        Objects.requireNonNull(usuarioId, "usuarioId es obligatorio");
        Objects.requireNonNull(fechaNegocio, "fechaNegocio es obligatorio");

        RecepcionOC recepcion = recepcionOCRepository.findByOrdenCompraIdAndFechaRecepcion(ordenCompraId, fechaNegocio)
                .orElseGet(() -> crearCabecera(ordenCompraId, almacenDestinoId, proveedorId, usuarioId, fechaNegocio, observaciones));
        if (recepcion.getGastosAdicionalesTotal() == null) {
            recepcion.setGastosAdicionalesTotal(java.math.BigDecimal.ZERO.setScale(6));
        }
        if (recepcion.getCriterioProrrateo() == null || recepcion.getCriterioProrrateo().isBlank()) {
            recepcion.setCriterioProrrateo("VALOR");
        }
        return recepcion;
    }

    @Override
    @Transactional(readOnly = true)
    public RecepcionOCResponseDTO obtenerRecepcionPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODIGO_RECEPCION_REQUERIDO");
        }

        RecepcionOC recepcion = recepcionOCRepository.findWithDetallesByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RECEPCION_NO_ENCONTRADA"));

        List<MovimientoInventarioResponseDTO> movimientos = movimientoInventarioRepository
                .findAllByRecepcionOcIdOrderByFechaIngresoAsc(recepcion.getId())
                .stream()
                .map(movimientoInventarioMapper::safeToResponseDTO)
                .toList();

        return recepcionOCMapper.toResponse(recepcion, movimientos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecepcionOCResponseDTO> listarRecepcionesPorOrden(Long ordenCompraId) {
        List<RecepcionOC> recepciones = recepcionOCRepository
                .findAllByOrdenCompra_IdOrderByFechaRecepcionAsc(ordenCompraId);

        return recepciones.stream()
                .map(recepcion -> {
                    List<MovimientoInventarioResponseDTO> movimientos = movimientoInventarioRepository
                            .findAllByRecepcionOcIdOrderByFechaIngresoAsc(recepcion.getId())
                            .stream()
                            .map(movimientoInventarioMapper::safeToResponseDTO)
                            .toList();
                    return recepcionOCMapper.toResponse(recepcion, movimientos);
                })
                .toList();
    }

    private RecepcionOC crearCabecera(Integer ordenCompraId,
                                      Integer almacenDestinoId,
                                      Integer proveedorId,
                                      Long usuarioId,
                                      LocalDate fechaNegocio,
                                      String observaciones) {
        String codigo = codigoRecepcionService.generarCodigo(fechaNegocio);
        RecepcionOC nueva = RecepcionOC.builder()
                .codigo(codigo)
                .fechaRecepcion(fechaNegocio)
                .ordenCompra(entityManager.getReference(OrdenCompra.class, ordenCompraId))
                .almacenDestino(entityManager.getReference(Almacen.class, almacenDestinoId))
                .usuario(entityManager.getReference(Usuario.class, usuarioId))
                .observaciones(observaciones)
                .build();
        if (proveedorId != null) {
            nueva.setProveedor(entityManager.getReference(Proveedor.class, proveedorId));
        }
        RecepcionOC guardada = recepcionOCRepository.saveAndFlush(nueva);
        log.info("Creando RecepcionOC codigo={} ocId={} fecha={}", codigo, ordenCompraId, fechaNegocio);
        return guardada;
    }
}
