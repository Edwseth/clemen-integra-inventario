package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EspecificacionCalidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecificacionCalidadRepository extends JpaRepository<EspecificacionCalidad, Long> {
    Page<EspecificacionCalidad> findByProducto_Id(Long productoId, Pageable pageable);
}