package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ObservacionProceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacionProcesoRepository extends JpaRepository<ObservacionProceso, Long> {
    List<ObservacionProceso> findByOrdenProduccionId(Long ordenProduccionId);
    void deleteByOrdenProduccionId(Long ordenProduccionId);
}
