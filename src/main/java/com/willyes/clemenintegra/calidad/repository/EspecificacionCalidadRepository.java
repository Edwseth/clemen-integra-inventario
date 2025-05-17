package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EspecificacionCalidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecificacionCalidadRepository extends JpaRepository<EspecificacionCalidad, Long> {
}