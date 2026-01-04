package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistEtapaTemplateRepository extends JpaRepository<ChecklistEtapaTemplate, Long> {
    List<ChecklistEtapaTemplate> findByEtapaPlantillaIdAndActivoTrueOrderByOrdenAsc(Long etapaPlantillaId);

    boolean existsByEtapaPlantillaIdAndActivoTrue(Long etapaPlantillaId);

    List<ChecklistEtapaTemplate> findByEtapaPlantillaId(Long etapaPlantillaId);
}
