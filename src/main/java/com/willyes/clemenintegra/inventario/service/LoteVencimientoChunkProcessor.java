package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
class LoteVencimientoChunkProcessor {

    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final InventoryVencidosProperties properties;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChunkResult process(List<Long> loteIds,
                               LocalDateTime cutoff,
                               LocalDateTime inicioDia,
                               LocalDateTime finDia,
                               LocalDateTime fechaMovimiento) {
        long actualizados = 0L;
        long movimientos = 0L;

        Long destinoIdConfig = properties.getMovimiento().isEnabled()
                ? properties.requireAlmacenDestinoId()
                : null;

        for (Long loteId : loteIds) {
            if (loteId == null) {
                continue;
            }
            Optional<LoteProducto> optional = loteProductoRepository.findByIdWithLock(loteId);
            if (optional.isEmpty()) {
                continue;
            }
            LoteProducto lote = optional.get();
            LocalDateTime fechaVencimiento = lote.getFechaVencimiento();
            if (fechaVencimiento == null || !fechaVencimiento.isBefore(cutoff)) {
                continue;
            }
            if (properties.getEstadoObjetivo() == lote.getEstado()) {
                continue;
            }

            lote.setEstado(properties.getEstadoObjetivo());
            actualizados++;

            if (properties.getMovimiento().isEnabled()) {
                Long motivoId = properties.getMovimiento().getMotivoId();
                if (motivoId == null) {
                    throw new IllegalStateException("inventory.vencidos.movimiento.motivoId es obligatorio cuando movimiento.enabled=true");
                }

                boolean existeMovimiento = movimientoInventarioService
                        .existeMovimientoVencimientoHoy(lote.getId(), motivoId, inicioDia, finDia);

                Integer destinoId = destinoIdConfig != null ? Math.toIntExact(destinoIdConfig) : null;

                if (!existeMovimiento) {
                    movimientoInventarioService.registrarRetiroPorVencimiento(lote, properties, fechaMovimiento);
                    movimientos++;
                    if (destinoId != null) {
                        lote.setAlmacen(entityManager.getReference(Almacen.class, destinoId));
                    }
                } else if (destinoId != null) {
                    Almacen actual = lote.getAlmacen();
                    if (actual == null || !Objects.equals(actual.getId(), destinoId)) {
                        lote.setAlmacen(entityManager.getReference(Almacen.class, destinoId));
                    }
                }
            }

            loteProductoRepository.save(lote);
        }

        return new ChunkResult(actualizados, movimientos);
    }

    record ChunkResult(long actualizados, long movimientos) { }
}
