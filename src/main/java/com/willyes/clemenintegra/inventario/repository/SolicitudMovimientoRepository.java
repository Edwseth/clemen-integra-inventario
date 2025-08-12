package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitudMovimientoRepository extends JpaRepository<SolicitudMovimiento, Long> {
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
            "where (:ordenId is null or op.id = :ordenId) " +
            "and (:estado is null or s.estado = :estado) " +
            "and (:desde is null or s.fechaSolicitud >= :desde) " +
            "and (:hasta is null or s.fechaSolicitud <= :hasta) " +
            "order by s.fechaSolicitud desc")
    List<SolicitudMovimiento> findWithDetalles(@Param("ordenId") Long ordenId,
                                               @Param("estado") EstadoSolicitudMovimiento estado,
                                               @Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta);
}
