package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CorridaMrpRepository extends JpaRepository<CorridaMrp, Long> {

    List<CorridaMrp> findByPlanProduccionSemanalId(Long planId);

    @EntityGraph(attributePaths = {
            "planProduccionSemanal",
            "detalles",
            "detalles.producto",
            "detalles.sugerencia",
            "detalles.sugerencia.detalleCorrida"
    })
    Optional<CorridaMrp> findWithDetallesById(Long id);
}
