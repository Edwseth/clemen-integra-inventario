package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface EvaluacionCalidadRepository extends JpaRepository<EvaluacionCalidad, Long> {
    Page<EvaluacionCalidad> findByResultado(ResultadoEvaluacion resultado, Pageable pageable);

    @EntityGraph(attributePaths = {"loteProducto.producto", "usuarioEvaluador"})
    @Query("SELECT e FROM EvaluacionCalidad e WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin")
    Page<EvaluacionCalidad> findAllByFechaEvaluacionBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);
}
