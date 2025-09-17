package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaLoteRepository extends JpaRepository<ReservaLote, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaLote r where r.solicitudMovimientoDetalle.id = :detalleId and r.lote.id = :loteId")
    Optional<ReservaLote> findByDetalleIdAndLoteIdForUpdate(@Param("detalleId") Long detalleId,
                                                            @Param("loteId") Long loteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaLote> findFirstBySolicitudMovimientoDetalleIdAndEstadoInOrderByIdAsc(Long detalleId,
                                                                                        Collection<EstadoReservaLote> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaLote> findFirstBySolicitudMovimientoDetalle_SolicitudMovimientoIdAndLoteIdAndEstadoInOrderByIdAsc(
            Long solicitudId,
            Long loteId,
            Collection<EstadoReservaLote> estados);

    @Query("select coalesce(sum(case when r.estado <> :estadoCancelada then " +
            "case when (r.cantidadReservada - r.cantidadConsumida) > 0 then (r.cantidadReservada - r.cantidadConsumida) else 0 end " +
            "else 0 end), 0) from ReservaLote r where r.lote.id = :loteId")
    BigDecimal sumPendienteByLoteId(@Param("loteId") Long loteId,
                                    @Param("estadoCancelada") EstadoReservaLote estadoCancelada);

    List<ReservaLote> findBySolicitudMovimientoDetalleId(Long detalleId);

    List<ReservaLote> findBySolicitudMovimientoDetalle_SolicitudMovimientoId(Long solicitudId);
}

