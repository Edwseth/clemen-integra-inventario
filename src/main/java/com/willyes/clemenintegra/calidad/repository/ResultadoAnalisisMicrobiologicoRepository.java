package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoAnalisisMicrobiologicoRepository extends JpaRepository<ResultadoAnalisisMicrobiologico, Long> {
    List<ResultadoAnalisisMicrobiologico> findByEvaluacionId(Long evaluacionId);
}

