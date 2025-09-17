package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudMovimientoDetalleRepository extends JpaRepository<SolicitudMovimientoDetalle, Long> {

    interface SolicitudDetalleCount {
        Long getSolicitudId();

        Long getCnt();
    }

    interface OpDetalleCount {
        Long getOpId();

        Long getCnt();
    }

    @Query("""
            select d.solicitudMovimiento.id as solicitudId, count(d.id) as cnt
            from SolicitudMovimientoDetalle d
            where d.solicitudMovimiento.id in :ids
            group by d.solicitudMovimiento.id
            """)
    List<SolicitudDetalleCount> countBySolicitudIds(@Param("ids") List<Long> ids);

    @Query("""
            select d.solicitudMovimiento.ordenProduccion.id as opId, count(d.id) as cnt
            from SolicitudMovimientoDetalle d
            where d.solicitudMovimiento.ordenProduccion.id in :opIds
            group by d.solicitudMovimiento.ordenProduccion.id
            """)
    List<OpDetalleCount> countByOpIds(@Param("opIds") List<Long> opIds);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudMovimientoDetalle> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudMovimientoDetalle> findFirstBySolicitudMovimientoIdAndLoteIdAndEstadoInOrderByIdAsc(
            Long solicitudId,
            Long loteId,
            Collection<EstadoSolicitudMovimientoDetalle> estados
    );

    long countBySolicitudMovimientoIdAndEstadoNot(Long solicitudId, EstadoSolicitudMovimientoDetalle estado);
}

