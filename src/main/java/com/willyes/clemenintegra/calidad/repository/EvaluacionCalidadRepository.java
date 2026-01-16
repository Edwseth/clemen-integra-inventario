package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaListadoDTO;
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
    @Query("SELECT e FROM EvaluacionCalidad e WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin ORDER BY e.fechaEvaluacion DESC")
    Page<EvaluacionCalidad> findAllByFechaEvaluacionBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    java.util.Optional<EvaluacionCalidad> findFirstByLoteProductoIdAndTipoEvaluacion(Long loteId, TipoEvaluacion tipoEvaluacion);

    @EntityGraph(attributePaths = {"loteProducto.producto", "usuarioEvaluador"})
    @Query("SELECT e FROM EvaluacionCalidad e")
    java.util.List<EvaluacionCalidad> findAllWithRelations();

    @Query("SELECT e FROM EvaluacionCalidad e " +
            "JOIN FETCH e.loteProducto l " +
            "JOIN FETCH l.producto p " +
            "JOIN FETCH e.usuarioEvaluador u " +
            "WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin")
    java.util.List<EvaluacionCalidad> findAllWithinFechaEvaluacion(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT new com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaListadoDTO(" +
            "e.id, l.id, e.fechaEvaluacion, l.codigoLote, p.nombre, e.resultado, e.tipoEvaluacion, " +
            "u.nombreCompleto, " +
            "com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad.EVALUADO, " +
            "l.estado, COUNT(a)) " +
            "FROM EvaluacionCalidad e " +
            "JOIN e.loteProducto l " +
            "JOIN l.producto p " +
            "JOIN e.usuarioEvaluador u " +
            "LEFT JOIN e.archivosAdjuntos a " +
            "WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin " +
            "GROUP BY e.id, l.id, e.fechaEvaluacion, l.codigoLote, p.nombre, e.resultado, e.tipoEvaluacion, u.nombreCompleto, l.estado",
            countQuery = "SELECT COUNT(e.id) FROM EvaluacionCalidad e " +
                    "WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin")
    Page<EvaluacionConsolidadaListadoDTO> findConsolidadoListado(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    boolean existsByLoteProductoIdAndTipoEvaluacion(Long loteId, TipoEvaluacion tipo);

    java.util.List<EvaluacionCalidad> findByLoteProductoId(Long loteId);

    java.util.List<EvaluacionCalidad> findByLoteProductoIdIn(java.util.List<Long> loteIds);

    @Query("SELECT DISTINCT e FROM EvaluacionCalidad e " +
            "LEFT JOIN FETCH e.archivosAdjuntos " +
            "JOIN FETCH e.usuarioEvaluador " +
            "JOIN FETCH e.loteProducto l " +
            "WHERE l.id = :loteId")
    java.util.List<EvaluacionCalidad> findByLoteProductoIdWithAdjuntos(@Param("loteId") Long loteId);

    @EntityGraph(attributePaths = {"loteProducto", "loteProducto.producto", "loteProducto.almacen", "usuarioEvaluador", "archivosAdjuntos"})
    java.util.Optional<EvaluacionCalidad> findByIdConRelaciones(Long id);
}
