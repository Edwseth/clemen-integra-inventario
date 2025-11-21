package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ControlProcesoProduccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlProcesoProduccionRepository extends JpaRepository<ControlProcesoProduccion, Long> {
    List<ControlProcesoProduccion> findByOrdenProduccionId(Long ordenProduccionId);
    void deleteByOrdenProduccionId(Long ordenProduccionId);
}
