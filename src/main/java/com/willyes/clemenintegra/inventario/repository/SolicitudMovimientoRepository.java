package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitudMovimientoRepository extends JpaRepository<SolicitudMovimiento, Long>, JpaSpecificationExecutor<SolicitudMovimiento> {
    List<SolicitudMovimiento> findByEstado(EstadoSolicitudMovimiento estado);
    List<SolicitudMovimiento> findByUsuarioResponsableId(Long usuarioResponsableId);

    @Query("select distinct s from SolicitudMovimiento s " +
            "left join fetch s.producto p " +
            "left join fetch p.unidadMedida um " +
            "left join fetch s.lote l " +
            "left join fetch s.almacenOrigen ao " +
            "left join fetch s.almacenDestino ad " +
            "left join fetch s.usuarioSolicitante us " +
            "left join fetch s.ordenProduccion op " +
            "left join fetch s.detalles det " +
            "left join fetch det.lote detLote " +
            "left join fetch det.almacenOrigen detAo " +
            "left join fetch det.almacenDestino detAd " +
            "where (:ordenId is null or op.id = :ordenId) " +
            "and (:estados is null or s.estado in :estados) " +
            "and (:desde is null or s.fechaSolicitud >= :desde) " +
            "and (:hasta is null or s.fechaSolicitud <= :hasta) " +
            "order by s.fechaSolicitud desc")
    List<SolicitudMovimiento> findWithDetalles(@Param("ordenId") Long ordenId,
                                               @Param("estados") List<EstadoSolicitudMovimiento> estados,
                                               @Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta);

    @Query("select s from SolicitudMovimiento s " +
            "left join fetch s.producto p " +
            "left join fetch p.unidadMedida um " +
            "left join fetch s.lote l " +
            "left join fetch s.almacenOrigen ao " +
            "left join fetch s.almacenDestino ad " +
            "left join fetch s.usuarioSolicitante us " +
            "left join fetch s.ordenProduccion op " +
            "left join fetch s.motivoMovimiento mm " +
            "left join fetch s.tipoMovimientoDetalle tmd " +
            "left join fetch s.detalles det " +
            "left join fetch det.lote detLote " +
            "left join fetch det.almacenOrigen detAo " +
            "left join fetch det.almacenDestino detAd " +
            "where s.id = :id")
    java.util.Optional<SolicitudMovimiento> findWithDetalles(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct s from SolicitudMovimiento s " +
            "left join fetch s.producto p " +
            "left join fetch p.unidadMedida um " +
            "left join fetch s.lote l " +
            "left join fetch s.almacenOrigen ao " +
            "left join fetch s.almacenDestino ad " +
            "left join fetch s.usuarioSolicitante us " +
            "left join fetch s.ordenProduccion op " +
            "left join fetch s.motivoMovimiento mm " +
            "left join fetch s.tipoMovimientoDetalle tmd " +
            "left join fetch s.detalles det " +
            "left join fetch det.lote detLote " +
            "left join fetch det.almacenOrigen detAo " +
            "left join fetch det.almacenDestino detAd " +
            "where s.id = :id")
    Optional<SolicitudMovimiento> findByIdWithLock(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"usuarioSolicitante", "ordenProduccion"})
    Page<SolicitudMovimiento> findAll(Specification<SolicitudMovimiento> spec, Pageable pageable);
}
