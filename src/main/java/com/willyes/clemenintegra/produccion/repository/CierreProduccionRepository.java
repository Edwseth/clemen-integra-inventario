package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CierreProduccionRepository extends JpaRepository<CierreProduccion, Long> {
    Page<CierreProduccion> findByOrdenProduccionId(Long ordenId, Pageable pageable);
}
