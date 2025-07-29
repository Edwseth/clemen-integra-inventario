package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudMovimientoRepository extends JpaRepository<SolicitudMovimiento, Long> {
    List<SolicitudMovimiento> findByEstado(EstadoSolicitudMovimiento estado);
    List<SolicitudMovimiento> findByUsuarioResponsableId(Long usuarioResponsableId);
}
