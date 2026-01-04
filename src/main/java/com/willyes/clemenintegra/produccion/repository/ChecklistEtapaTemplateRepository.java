package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChecklistEtapaTemplateRepository extends JpaRepository<ChecklistEtapaTemplate, Long> {
    List<ChecklistEtapaTemplate> findByEtapaPlantillaIdAndActivoTrueOrderByOrdenAsc(Long etapaPlantillaId);

    @Query("select c from ChecklistEtapaTemplate c " +
            "where c.etapaPlantilla.id = :etapaPlantillaId and c.activo = true order by c.orden asc")
    List<ChecklistEtapaTemplate> findActiveByEtapaPlantillaIdOrderByOrdenAsc(Long etapaPlantillaId);

    boolean existsByEtapaPlantillaIdAndActivoTrue(Long etapaPlantillaId);

    List<ChecklistEtapaTemplate> findByEtapaPlantillaId(Long etapaPlantillaId);
}
