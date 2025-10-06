package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.RecepcionOCRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

import com.willyes.clemenintegra.shared.model.Usuario;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionOCServiceImpl implements RecepcionOCService {

    private final RecepcionOCRepository recepcionOCRepository;
    private final CodigoRecepcionService codigoRecepcionService;
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

        return recepcionOCRepository.findByOrdenCompraIdAndFechaRecepcion(ordenCompraId, fechaNegocio)
                .orElseGet(() -> crearCabecera(ordenCompraId, almacenDestinoId, proveedorId, usuarioId, fechaNegocio, observaciones));
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
