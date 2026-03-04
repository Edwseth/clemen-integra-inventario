package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OcServicioEjecucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OcServicioEjecucionRepository extends JpaRepository<OcServicioEjecucion, Long> {
    List<OcServicioEjecucion> findByOrdenCompra_IdOrderByFechaEjecucionDesc(Long ordenCompraId);
}
