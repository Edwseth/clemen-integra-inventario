package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.PlantillaCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaCampoRepository extends JpaRepository<PlantillaCampo, Long> {
    boolean existsByPlantilla_IdAndCodigo(Long plantillaId, String codigo);

    List<PlantillaCampo> findByPlantilla_IdOrderByOrdenAsc(Long plantillaId);
}
