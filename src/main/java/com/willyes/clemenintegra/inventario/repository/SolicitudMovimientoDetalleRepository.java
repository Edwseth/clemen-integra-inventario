package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudMovimientoDetalleRepository extends JpaRepository<SolicitudMovimientoDetalle, Long> {
}

