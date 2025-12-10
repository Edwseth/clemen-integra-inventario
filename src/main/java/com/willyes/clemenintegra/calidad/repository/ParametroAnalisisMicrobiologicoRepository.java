package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParametroAnalisisMicrobiologicoRepository extends JpaRepository<ParametroAnalisisMicrobiologico, Long> {
    List<ParametroAnalisisMicrobiologico> findByPlantillaIdOrderByOrdenAsc(Long plantillaId);
}

