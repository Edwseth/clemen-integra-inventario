package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
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
            "detalles.producto.categoriaProducto",
            "detalles.sugerencia",
            "detalles.sugerencia.detalleCorrida",
            "detalles.sugerencia.detalleCorrida.producto",
            "detalles.sugerencia.detalleCorrida.producto.categoriaProducto"
    })
    Optional<CorridaMrp> findWithDetallesById(Long id);

    @EntityGraph(attributePaths = {
            "planProduccionSemanal",
            "detalles",
            "detalles.producto",
            "detalles.producto.categoriaProducto",
            "detalles.sugerencia",
            "detalles.sugerencia.detalleCorrida",
            "detalles.sugerencia.detalleCorrida.producto",
            "detalles.sugerencia.detalleCorrida.producto.categoriaProducto"
    })
    Optional<CorridaMrp> findByIdWithGraph(Long id);

    Optional<CorridaMrp> findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(
            PlanProduccionSemanal plan, EstadoCorridaMrp estado
    );
}
