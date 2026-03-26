package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
class LoteVencimientoChunkProcessor {

    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final InventoryVencidosProperties properties;
    private final EntityManager entityManager;
    private final TransactionTemplate perLoteTxTemplate;

    LoteVencimientoChunkProcessor(LoteProductoRepository loteProductoRepository,
                                  MovimientoInventarioService movimientoInventarioService,
                                  InventoryVencidosProperties properties,
                                  EntityManager entityManager,
                                  PlatformTransactionManager transactionManager) {
        this.loteProductoRepository = loteProductoRepository;
        this.movimientoInventarioService = movimientoInventarioService;
        this.properties = properties;
        this.entityManager = entityManager;
        this.perLoteTxTemplate = new TransactionTemplate(transactionManager);
        this.perLoteTxTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChunkResult process(List<Long> loteIds,
                               LocalDateTime cutoff,
                               LocalDateTime inicioDia,
                               LocalDateTime finDia,
                               LocalDateTime fechaMovimiento,
                               Long destinoIdResuelto,
                               Usuario usuarioSistema) {
        long actualizados = 0L;
        long movimientos = 0L;

        Long destinoIdConfig = properties.getMovimiento().isEnabled() ? destinoIdResuelto : null;

        for (Long loteId : loteIds) {
            try {
                if (loteId == null) {
                    continue;
                }
                SingleLoteResult single = perLoteTxTemplate.execute(status ->
                        processSingleLote(loteId, cutoff, inicioDia, finDia, fechaMovimiento, destinoIdConfig, usuarioSistema));
                if (single == null) {
                    continue;
                }
                actualizados += single.actualizados();
                movimientos += single.movimientos();
            } catch (Exception ex) {
                log.error("[VENCIDOS][LOTE_ERROR] loteId={} mensaje={}", loteId, ex.getMessage(), ex);
            }
        }

        return new ChunkResult(actualizados, movimientos);
    }

    private SingleLoteResult processSingleLote(Long loteId,
                                               LocalDateTime cutoff,
                                               LocalDateTime inicioDia,
                                               LocalDateTime finDia,
                                               LocalDateTime fechaMovimiento,
                                               Long destinoIdConfig,
                                               Usuario usuarioSistema) {
        Optional<LoteProducto> optional = loteProductoRepository.findByIdWithLock(loteId);
        if (optional.isEmpty()) {
            log.info("[VENCIDOS][LOTE] loteId={} candidato=false razon=NO_ENCONTRADO", loteId);
            return SingleLoteResult.EMPTY;
        }
        LoteProducto lote = optional.get();
        Integer productoId = lote.getProducto() != null ? lote.getProducto().getId() : null;
        Integer almacenOrigenId = lote.getAlmacen() != null ? lote.getAlmacen().getId() : null;
        Integer almacenDestinoId = destinoIdConfig != null ? Math.toIntExact(destinoIdConfig) : null;

        LocalDateTime fechaVencimiento = lote.getFechaVencimiento();
        boolean candidato = fechaVencimiento != null && fechaVencimiento.isBefore(cutoff);
        if (!candidato) {
            log.info("[VENCIDOS][LOTE] loteId={} codigoLote={} productoId={} almacenOrigenId={} almacenDestinoId={} candidato=false razon=NO_VENCIDO",
                    loteId, lote.getCodigoLote(), productoId, almacenOrigenId, almacenDestinoId);
            return SingleLoteResult.EMPTY;
        }

        if (properties.getEstadoObjetivo() == lote.getEstado()) {
            log.info("[VENCIDOS][LOTE] loteId={} codigoLote={} productoId={} almacenOrigenId={} almacenDestinoId={} candidato=true cambioEstado=false razon=ESTADO_YA_OBJETIVO",
                    loteId, lote.getCodigoLote(), productoId, almacenOrigenId, almacenDestinoId);
            return SingleLoteResult.EMPTY;
        }

        lote.setEstado(properties.getEstadoObjetivo());
        boolean movimientoCreado = false;
        boolean conflictoUnicidad = false;
        boolean mergeAplicado = false;

        if (properties.getMovimiento().isEnabled()) {
            Long motivoId = properties.getMovimiento().getMotivoId();
            if (motivoId == null) {
                throw new IllegalStateException("inventory.vencidos.movimiento.motivoId es obligatorio cuando movimiento.enabled=true");
            }

            boolean existeMovimiento = movimientoInventarioService
                    .existeMovimientoVencimientoHoy(lote.getId(), motivoId, inicioDia, finDia);

            if (!existeMovimiento) {
                movimientoInventarioService.registrarRetiroPorVencimiento(lote, properties, destinoIdConfig, usuarioSistema, fechaMovimiento);
                movimientoCreado = true;
            }

            if (almacenDestinoId != null) {
                Optional<LoteProducto> destinoExistente = loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(
                        lote.getProducto().getId(),
                        lote.getCodigoLote(),
                        almacenDestinoId);

                if (destinoExistente.isPresent() && !Objects.equals(destinoExistente.get().getId(), lote.getId())) {
                    conflictoUnicidad = true;
                    mergeAplicado = true;
                    LoteProducto destino = destinoExistente.get();
                    BigDecimal stockOrigen = defaultDecimal(lote.getStockLote());
                    BigDecimal reservadoOrigen = defaultDecimal(lote.getStockReservado());
                    destino.setStockLote(defaultDecimal(destino.getStockLote()).add(stockOrigen));
                    destino.setStockReservado(defaultDecimal(destino.getStockReservado()).add(reservadoOrigen));
                    destino.setAgotado(false);
                    destino.setFechaAgotado(null);
                    loteProductoRepository.save(destino);

                    lote.setStockLote(BigDecimal.ZERO);
                    lote.setStockReservado(BigDecimal.ZERO);
                    lote.setAgotado(true);
                    lote.setFechaAgotado(LocalDateTime.now());
                } else {
                    Almacen actual = lote.getAlmacen();
                    if (actual == null || !Objects.equals(actual.getId(), almacenDestinoId)) {
                        lote.setAlmacen(entityManager.getReference(Almacen.class, almacenDestinoId));
                    }
                }
            }
        }

        loteProductoRepository.save(lote);
        log.info("[VENCIDOS][LOTE] loteId={} codigoLote={} productoId={} almacenOrigenId={} almacenDestinoId={} candidato=true cambioEstado=true movimientoCreado={} conflictoUnicidad={} mergeAplicado={}",
                lote.getId(), lote.getCodigoLote(), productoId, almacenOrigenId, almacenDestinoId,
                movimientoCreado, conflictoUnicidad, mergeAplicado);
        return new SingleLoteResult(1, movimientoCreado ? 1 : 0);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    record SingleLoteResult(long actualizados, long movimientos) {
        private static final SingleLoteResult EMPTY = new SingleLoteResult(0, 0);
    }

    record ChunkResult(long actualizados, long movimientos) { }
}
