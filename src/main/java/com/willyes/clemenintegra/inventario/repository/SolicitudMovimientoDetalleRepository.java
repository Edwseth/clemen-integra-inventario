package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudMovimientoDetalleRepository extends JpaRepository<SolicitudMovimientoDetalle, Long> {

    interface SolicitudDetalleCount {
        Long getSolicitudId();

        Long getCnt();
    }

    @Query("""
            select d.solicitudMovimiento.id as solicitudId, count(d.id) as cnt
            from SolicitudMovimientoDetalle d
            where d.solicitudMovimiento.id in :ids
            group by d.solicitudMovimiento.id
            """)
    List<SolicitudDetalleCount> countBySolicitudIds(@Param("ids") List<Long> ids);
}

