package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairResultDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservaLoteRepairService {

    private static final Logger log = LoggerFactory.getLogger(ReservaLoteRepairService.class);

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final ReservaLoteRepository reservaLoteRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    private final TransactionTemplate transactionTemplate;

    public ReservaLoteRepairResultDTO repararReservasLote(ReservaLoteRepairRequestDTO request) {
        if (request == null || (isEmpty(request.detalleIds()) && request.ordenProduccionId() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar detalleIds o ordenProduccionId para reparar reservas");
        }

        int batchSize = request.batchSize() != null && request.batchSize() > 0
                ? request.batchSize()
                : DEFAULT_BATCH_SIZE;

        List<ReservaLote> reservas = cargarReservas(request);
        if (reservas.isEmpty()) {
            return new ReservaLoteRepairResultDTO(0, 0, 0, 0, List.of());
        }

        List<Long> actualizadas = new ArrayList<>();
        int pendientes = 0;
        int sinCambios = 0;

        for (int i = 0; i < reservas.size(); i += batchSize) {
            List<ReservaLote> batch = reservas.subList(i, Math.min(i + batchSize, reservas.size()));
            RepairBatchResult result = transactionTemplate.execute(status -> repararBatch(batch));
            if (result != null) {
                actualizadas.addAll(result.reservasActualizadas());
                pendientes += result.pendientes();
                sinCambios += result.sinCambios();
            }
        }

        return new ReservaLoteRepairResultDTO(reservas.size(),
                actualizadas.size(),
                sinCambios,
                pendientes,
                actualizadas);
    }

    private RepairBatchResult repararBatch(List<ReservaLote> batch) {
        List<Long> actualizadas = new ArrayList<>();
        int pendientes = 0;
        int sinCambios = 0;

        for (ReservaLote reserva : batch) {
            if (reserva == null || reserva.getId() == null) {
                continue;
            }

            Long reservaId = reserva.getId();
            SolicitudMovimientoDetalle detalle = cargarDetalle(reserva);
            if (detalle == null || detalle.getId() == null) {
                pendientes++;
                log.warn("REPAIR_RESERVA_LOTE pendiente: reservaId={} sin detalle asociado", reservaId);
                continue;
            }

            LoteProducto loteActual = cargarLoteActual(reserva);
            if (loteActual == null) {
                pendientes++;
                log.warn("REPAIR_RESERVA_LOTE pendiente: reservaId={} sin lote actual", reservaId);
                continue;
            }

            Integer productoId = loteActual.getProducto() != null ? loteActual.getProducto().getId() : null;
            String codigoLote = loteActual.getCodigoLote();
            Integer almacenOrigenId = resolverAlmacenOrigenId(detalle, loteActual);

            if (productoId == null || almacenOrigenId == null || !StringUtils.hasText(codigoLote)) {
                pendientes++;
                log.warn("REPAIR_RESERVA_LOTE pendiente: reservaId={} detalleId={} productoId={} almacenOrigenId={} codigoLote={}",
                        reservaId, detalle.getId(), productoId, almacenOrigenId, codigoLote);
                continue;
            }

            Optional<LoteProducto> loteCorrectoOpt = loteProductoRepository
                    .findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(productoId, codigoLote, almacenOrigenId);

            if (loteCorrectoOpt.isEmpty()) {
                pendientes++;
                log.warn("REPAIR_RESERVA_LOTE pendiente: reservaId={} detalleId={} no existe loteOrigen productoId={} almacenOrigenId={} codigoLote={}",
                        reservaId, detalle.getId(), productoId, almacenOrigenId, codigoLote);
                continue;
            }

            LoteProducto loteCorrecto = loteCorrectoOpt.get();
            if (Objects.equals(loteCorrecto.getId(), loteActual.getId())) {
                sinCambios++;
                continue;
            }

            Long loteAnteriorId = loteActual.getId();
            Integer almacenAnteriorId = loteActual.getAlmacen() != null ? loteActual.getAlmacen().getId() : null;

            reserva.setLote(loteCorrecto);
            reservaLoteRepository.save(reserva);

            recalcularStockReservado(loteActual);
            recalcularStockReservado(loteCorrecto);

            actualizadas.add(reservaId);
            log.info("REPAIR_RESERVA_LOTE reasignada reservaId={} detalleId={} loteAnteriorId={} loteNuevoId={} almacenAnteriorId={} almacenNuevoId={}",
                    reservaId,
                    detalle.getId(),
                    loteAnteriorId,
                    loteCorrecto.getId(),
                    almacenAnteriorId,
                    loteCorrecto.getAlmacen() != null ? loteCorrecto.getAlmacen().getId() : null);
        }

        return new RepairBatchResult(actualizadas, pendientes, sinCambios);
    }

    private List<ReservaLote> cargarReservas(ReservaLoteRepairRequestDTO request) {
        if (!isEmpty(request.detalleIds())) {
            return reservaLoteRepository.findByEstadoAndSolicitudMovimientoDetalleIdIn(
                    EstadoReservaLote.ACTIVA, request.detalleIds());
        }
        return reservaLoteRepository.findByEstadoAndSolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(
                EstadoReservaLote.ACTIVA, request.ordenProduccionId());
    }

    private SolicitudMovimientoDetalle cargarDetalle(ReservaLote reserva) {
        Long detalleId = reserva.getSolicitudMovimientoDetalle() != null
                ? reserva.getSolicitudMovimientoDetalle().getId()
                : null;
        if (detalleId == null) {
            return null;
        }
        return solicitudMovimientoDetalleRepository.findById(detalleId).orElse(null);
    }

    private LoteProducto cargarLoteActual(ReservaLote reserva) {
        Long loteId = reserva.getLote() != null ? reserva.getLote().getId() : null;
        if (loteId == null) {
            return null;
        }
        return loteProductoRepository.findByIdForUpdate(loteId).orElse(null);
    }

    private Integer resolverAlmacenOrigenId(SolicitudMovimientoDetalle detalle, LoteProducto loteActual) {
        return Optional.ofNullable(detalle.getAlmacenOrigen())
                .map(Almacen::getId)
                .orElseGet(() -> Optional.ofNullable(detalle.getSolicitudMovimiento())
                        .map(SolicitudMovimiento::getAlmacenOrigen)
                        .map(Almacen::getId)
                        .orElseGet(() -> loteActual.getAlmacen() != null ? loteActual.getAlmacen().getId() : null));
    }

    private void recalcularStockReservado(LoteProducto lote) {
        if (lote == null || lote.getId() == null) {
            return;
        }
        BigDecimal pendiente = Optional.ofNullable(reservaLoteRepository
                        .sumPendienteActivaByLoteId(lote.getId(), EstadoReservaLote.ACTIVA))
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        lote.setStockReservado(pendiente);
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private record RepairBatchResult(List<Long> reservasActualizadas, int pendientes, int sinCambios) {
        private RepairBatchResult {
            reservasActualizadas = reservasActualizadas != null ? reservasActualizadas : Collections.emptyList();
        }
    }
}
