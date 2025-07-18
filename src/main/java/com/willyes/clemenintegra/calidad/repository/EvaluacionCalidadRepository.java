package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionCalidadRepository extends JpaRepository<EvaluacionCalidad, Long> {
    Page<EvaluacionCalidad> findByResultado(ResultadoEvaluacion resultado, Pageable pageable);
}
