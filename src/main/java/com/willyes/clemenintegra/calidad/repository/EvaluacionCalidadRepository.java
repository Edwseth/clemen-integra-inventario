package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
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

    @Query("SELECT DISTINCT e FROM EvaluacionCalidad e " +
            "LEFT JOIN FETCH e.archivosAdjuntos " +
            "JOIN FETCH e.usuarioEvaluador " +
            "JOIN FETCH e.loteProducto l " +
            "JOIN FETCH l.producto p " +
            "WHERE (:inicio IS NULL OR e.fechaEvaluacion >= :inicio) " +
            "AND (:fin IS NULL OR e.fechaEvaluacion <= :fin) " +
            "AND (:estado IS NULL OR l.estado = :estado)")
    java.util.List<EvaluacionCalidad> findAllForExcel(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoLote estado);

    @Query("SELECT e FROM EvaluacionCalidad e " +
            "JOIN FETCH e.loteProducto l " +
            "JOIN FETCH l.producto p " +
            "JOIN FETCH e.usuarioEvaluador u " +
            "WHERE e.fechaEvaluacion BETWEEN :inicio AND :fin")
    java.util.List<EvaluacionCalidad> findAllWithinFechaEvaluacion(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @EntityGraph(attributePaths = {"loteProducto", "loteProducto.producto", "usuarioEvaluador", "archivosAdjuntos"})
    java.util.List<EvaluacionCalidad> findByLoteProductoIdIn(java.util.List<Long> loteIds);

    boolean existsByLoteProductoIdAndTipoEvaluacion(Long loteId, TipoEvaluacion tipo);

    java.util.List<EvaluacionCalidad> findByLoteProductoId(Long loteId);

    @EntityGraph(attributePaths = {"loteProducto", "loteProducto.producto", "usuarioEvaluador", "archivosAdjuntos"})
    @Query("SELECT e FROM EvaluacionCalidad e WHERE e.loteProducto.id IN :loteIds")
    java.util.List<EvaluacionCalidad> findByLoteProductoIdInWithRelacion(@Param("loteIds") java.util.List<Long> loteIds);

    @Query("SELECT DISTINCT e FROM EvaluacionCalidad e " +
            "LEFT JOIN FETCH e.archivosAdjuntos " +
            "JOIN FETCH e.usuarioEvaluador " +
            "JOIN FETCH e.loteProducto l " +
            "WHERE l.id = :loteId")
    java.util.List<EvaluacionCalidad> findByLoteProductoIdWithAdjuntos(@Param("loteId") Long loteId);

    @Override
    @EntityGraph(attributePaths = {"loteProducto", "loteProducto.producto", "usuarioEvaluador", "archivosAdjuntos"})
    Optional<EvaluacionCalidad> findById(Long id);
}
