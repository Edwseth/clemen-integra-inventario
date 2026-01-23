package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CorridaMrpRepository extends JpaRepository<CorridaMrp, Long> {

    List<CorridaMrp> findByPlanProduccionSemanalId(Long planId);

    @EntityGraph(attributePaths = {
            "planProduccionSemanal",
            "detalles",
            "detalles.producto",
            "detalles.producto.unidadMedida",
            "detalles.producto.categoriaProducto",
            "detalles.sugerencia",
            "detalles.sugerencia.detalleCorrida",
            "detalles.sugerencia.detalleCorrida.producto",
            "detalles.sugerencia.detalleCorrida.producto.unidadMedida",
            "detalles.sugerencia.detalleCorrida.producto.categoriaProducto"
    })
    Optional<CorridaMrp> findWithDetallesById(Long id);

    @EntityGraph(attributePaths = {
            "planProduccionSemanal",
            "detalles",
            "detalles.producto",
            "detalles.producto.unidadMedida",
            "detalles.producto.categoriaProducto",
            "detalles.sugerencia",
            "detalles.sugerencia.detalleCorrida",
            "detalles.sugerencia.detalleCorrida.producto",
            "detalles.sugerencia.detalleCorrida.producto.unidadMedida",
            "detalles.sugerencia.detalleCorrida.producto.categoriaProducto"
    })
    @Query("select c from CorridaMrp c where c.id = :id")
    Optional<CorridaMrp> findWithGraphById(@Param("id") Long id);

    Optional<CorridaMrp> findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(
            PlanProduccionSemanal plan, EstadoCorridaMrp estado
    );
}
