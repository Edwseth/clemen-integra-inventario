package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservaLoteService {

    private static final Logger log = LoggerFactory.getLogger(ReservaLoteService.class);

    private final ReservaLoteRepository reservaLoteRepository;
    private final LoteProductoRepository loteProductoRepository;

    @Transactional
    public void sincronizarReservasSolicitud(SolicitudMovimiento solicitud) {
        if (solicitud == null || solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            return;
        }
        for (SolicitudMovimientoDetalle detalle : solicitud.getDetalles()) {
            if (detalle == null || detalle.getId() == null || detalle.getLote() == null) {
                continue;
            }
            crearOActualizarDesdeDetalle(detalle);
        }
    }

    @Transactional
    public ReservaLote consumirReserva(SolicitudMovimiento solicitud,
                                       SolicitudMovimientoDetalle detalle,
                                       LoteProducto lote,
                                       BigDecimal cantidad) {
        if (lote == null || lote.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_LOTE_INVALIDA");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_CANTIDAD_INVALIDA");
        }

        BigDecimal normalizada = cantidad.setScale(6, RoundingMode.HALF_UP);
        ReservaLote reserva = obtenerReservaParaConsumo(solicitud, detalle, lote.getId());

        BigDecimal pendiente = calcularPendiente(reserva);
        if (pendiente.compareTo(normalizada) < 0) {
            log.warn("RESERVA_INSUFICIENTE: reservaId={} pendiente={} solicitado={} solicitudId={} loteId={}",
                    reserva.getId(), pendiente, normalizada,
                    solicitud != null ? solicitud.getId() : null, lote.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESERVA_INSUFICIENTE");
        }

        BigDecimal nuevoConsumido = reserva.getCantidadConsumida().add(normalizada);
        if (nuevoConsumido.compareTo(reserva.getCantidadReservada()) >= 0) {
            reserva.setCantidadConsumida(reserva.getCantidadReservada());
            reserva.setEstado(EstadoReservaLote.CONSUMIDA);
        } else {
            reserva.setCantidadConsumida(nuevoConsumido);
        }

        ReservaLote actualizada = reservaLoteRepository.save(reserva);
        recalcularStockReservado(lote);
        return actualizada;
    }

    private ReservaLote obtenerReservaParaConsumo(SolicitudMovimiento solicitud,
                                                  SolicitudMovimientoDetalle detalle,
                                                  Long loteId) {
        if (detalle != null) {
            return reservaLoteRepository
                    .findFirstBySolicitudMovimientoDetalleIdAndEstadoInOrderByIdAsc(
                            detalle.getId(), List.of(EstadoReservaLote.ACTIVA))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "RESERVA_NO_ENCONTRADA"));
        }

        Long solicitudId = solicitud != null ? solicitud.getId() : null;
        if (solicitudId == null || loteId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_PARAMETROS_INSUFICIENTES");
        }

        return reservaLoteRepository
                .findFirstBySolicitudMovimientoDetalle_SolicitudMovimientoIdAndLoteIdAndEstadoInOrderByIdAsc(
                        solicitudId, loteId, List.of(EstadoReservaLote.ACTIVA))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "RESERVA_NO_ENCONTRADA"));
    }

    @Transactional
    public ReservaLote crearOActualizarDesdeDetalle(SolicitudMovimientoDetalle detalle) {
        if (detalle == null || detalle.getId() == null || detalle.getLote() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_RESERVA_INVALIDO");
        }

        Long loteId = detalle.getLote().getId();
        LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

        BigDecimal cantidad = Optional.ofNullable(detalle.getCantidad())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        if (atendida.compareTo(cantidad) > 0) {
            atendida = cantidad;
        }

        Optional<ReservaLote> existenteOpt = reservaLoteRepository
                .findByDetalleIdAndLoteIdForUpdate(detalle.getId(), loteId);

        BigDecimal totalPendiente = Optional.ofNullable(reservaLoteRepository
                        .sumPendienteByLoteId(loteId, EstadoReservaLote.CANCELADA))
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal pendienteActual = existenteOpt
                .map(this::calcularPendiente)
                .orElse(BigDecimal.ZERO);

        BigDecimal pendienteOtros = totalPendiente.subtract(pendienteActual);
        if (pendienteOtros.compareTo(BigDecimal.ZERO) < 0) {
            pendienteOtros = BigDecimal.ZERO;
        }

        BigDecimal pendienteNuevo = cantidad.subtract(atendida);
        if (pendienteNuevo.compareTo(BigDecimal.ZERO) < 0) {
            pendienteNuevo = BigDecimal.ZERO;
        }

        BigDecimal stockLote = Optional.ofNullable(lote.getStockLote())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal disponible = stockLote.subtract(pendienteOtros);
        if (disponible.compareTo(BigDecimal.ZERO) < 0) {
            disponible = BigDecimal.ZERO;
        }

        if (pendienteNuevo.compareTo(disponible) > 0) {
            BigDecimal faltante = pendienteNuevo.subtract(disponible).setScale(6, RoundingMode.HALF_UP);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "STOCK_INSUFICIENTE: faltan " + faltante);
        }

        ReservaLote reserva = existenteOpt.orElseGet(() -> ReservaLote.builder()
                .lote(lote)
                .solicitudMovimientoDetalle(detalle)
                .build());

        reserva.setCantidadReservada(cantidad);
        reserva.setCantidadConsumida(atendida);
        reserva.setEstado(atendida.compareTo(cantidad) >= 0
                ? EstadoReservaLote.CONSUMIDA
                : EstadoReservaLote.ACTIVA);

        ReservaLote guardada = reservaLoteRepository.save(reserva);
        recalcularStockReservado(lote);
        return guardada;
    }

    private BigDecimal calcularPendiente(ReservaLote reserva) {
        BigDecimal reservada = Optional.ofNullable(reserva.getCantidadReservada()).orElse(BigDecimal.ZERO);
        BigDecimal consumida = Optional.ofNullable(reserva.getCantidadConsumida()).orElse(BigDecimal.ZERO);
        BigDecimal pendiente = reservada.subtract(consumida);
        if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
            pendiente = BigDecimal.ZERO;
        }
        return pendiente.setScale(6, RoundingMode.HALF_UP);
    }

    private void recalcularStockReservado(LoteProducto lote) {
        BigDecimal totalPendiente = Optional.ofNullable(reservaLoteRepository
                        .sumPendienteByLoteId(lote.getId(), EstadoReservaLote.CANCELADA))
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        lote.setStockReservado(totalPendiente);
        BigDecimal stock = Optional.ofNullable(lote.getStockLote())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        if (totalPendiente.compareTo(stock) > 0) {
            log.warn("STOCK_RESERVADO_SUPERA_STOCK: loteId={} reservado={} stock={}",
                    lote.getId(), totalPendiente, stock);
        }
    }
}

