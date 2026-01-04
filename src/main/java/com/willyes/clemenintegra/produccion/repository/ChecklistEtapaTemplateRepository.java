package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChecklistEtapaTemplateRepository extends JpaRepository<ChecklistEtapaTemplate, Long> {
    List<ChecklistEtapaTemplate> findByEtapaPlantillaIdAndActivoTrueOrderByOrdenAsc(Long etapaPlantillaId);

    List<ChecklistEtapaTemplate> findByEtapaPlantillaIdOrderByOrdenAsc(Long etapaPlantillaId);

    @Query("select c from ChecklistEtapaTemplate c " +
            "where c.etapaPlantilla.id = :etapaPlantillaId and c.activo = true order by c.orden asc")
    List<ChecklistEtapaTemplate> findActiveByEtapaPlantillaIdOrderByOrdenAsc(Long etapaPlantillaId);

    boolean existsByEtapaPlantillaIdAndActivoTrue(Long etapaPlantillaId);

    List<ChecklistEtapaTemplate> findByEtapaPlantillaId(Long etapaPlantillaId);

    @Query("select case when count(c) > 0 then true else false end from ChecklistEtapaTemplate c " +
            "where c.etapaPlantilla.id = :etapaPlantillaId and c.activo = true and c.nombreItem <> :placeholder")
    boolean existsOperativosActivosByEtapaPlantillaId(Long etapaPlantillaId, String placeholder);

    @Query("select c from ChecklistEtapaTemplate c " +
            "where c.etapaPlantilla.id = :etapaPlantillaId and c.activo = true and c.nombreItem <> :placeholder " +
            "order by c.orden asc")
    List<ChecklistEtapaTemplate> findOperativosActivosByEtapaPlantillaId(Long etapaPlantillaId, String placeholder);
}
