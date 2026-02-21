package com.willyes.clemenintegra.inventario.regularizacion.repository;

import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import com.willyes.clemenintegra.inventario.regularizacion.repository.projection.VariacionRegularizacionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegularizacionTrazabilidadRepository extends JpaRepository<RegularizacionTrazabilidad, Long> {
    Optional<RegularizacionTrazabilidad> findByIdempotencyKey(String idempotencyKey);

    boolean existsByOrdenProduccionId(Long ordenProduccionId);

    @Query(value = """
            SELECT t.id AS regularizacionId,
                   t.orden_produccion_id AS ordenProduccionId,
                   t.cantidad_programada AS cantidadProgramada,
                   t.cantidad_real AS cantidadReal,
                   t.diferencia AS diferencia,
                   t.ajustar_pt AS ajustarPt,
                   t.documento_referencia AS documentoReferencia,
                   t.observaciones AS observaciones,
                   t.usuario_id AS usuarioId,
                   t.fecha_ingreso AS fechaIngreso,
                   EXISTS(SELECT 1 FROM regularizacion_detalle rd WHERE rd.regularizacion_id = t.id) AS tieneDetalle
            FROM (
              SELECT rt.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            ) t
            WHERE t.rn = 1
              AND (:fechaInicio IS NULL OR t.fecha_ingreso >= :fechaInicio)
              AND (:fechaFin IS NULL OR t.fecha_ingreso <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR t.orden_produccion_id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR t.diferencia <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND t.diferencia > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND t.diferencia < 0)
                   OR (:tipoVariacion = 'CERO' AND t.diferencia = 0))
            ORDER BY t.fecha_ingreso DESC, t.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM (
              SELECT rt.id,
                     rt.orden_produccion_id,
                     rt.fecha_ingreso,
                     rt.diferencia,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            ) t
            WHERE t.rn = 1
              AND (:fechaInicio IS NULL OR t.fecha_ingreso >= :fechaInicio)
              AND (:fechaFin IS NULL OR t.fecha_ingreso <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR t.orden_produccion_id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR t.diferencia <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND t.diferencia > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND t.diferencia < 0)
                   OR (:tipoVariacion = 'CERO' AND t.diferencia = 0))
            """,
            nativeQuery = true)
    Page<VariacionRegularizacionProjection> findUltimasVariacionesPorOP(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                                        @Param("fechaFin") LocalDateTime fechaFin,
                                                                        @Param("ordenProduccionId") Long ordenProduccionId,
                                                                        @Param("soloConVariacion") boolean soloConVariacion,
                                                                        @Param("tipoVariacion") String tipoVariacion,
                                                                        Pageable pageable);

    @Query(value = """
            SELECT t.id AS regularizacionId,
                   t.orden_produccion_id AS ordenProduccionId,
                   t.cantidad_programada AS cantidadProgramada,
                   t.cantidad_real AS cantidadReal,
                   t.diferencia AS diferencia,
                   t.ajustar_pt AS ajustarPt,
                   t.documento_referencia AS documentoReferencia,
                   t.observaciones AS observaciones,
                   t.usuario_id AS usuarioId,
                   t.fecha_ingreso AS fechaIngreso,
                   EXISTS(SELECT 1 FROM regularizacion_detalle rd WHERE rd.regularizacion_id = t.id) AS tieneDetalle
            FROM (
              SELECT rt.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            ) t
            WHERE t.rn = 1
              AND (:fechaInicio IS NULL OR t.fecha_ingreso >= :fechaInicio)
              AND (:fechaFin IS NULL OR t.fecha_ingreso <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR t.orden_produccion_id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR t.diferencia <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND t.diferencia > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND t.diferencia < 0)
                   OR (:tipoVariacion = 'CERO' AND t.diferencia = 0))
            ORDER BY t.fecha_ingreso DESC, t.id DESC
            """, nativeQuery = true)
    List<VariacionRegularizacionProjection> findUltimasVariacionesPorOPSinPaginacion(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                                                      @Param("fechaFin") LocalDateTime fechaFin,
                                                                                      @Param("ordenProduccionId") Long ordenProduccionId,
                                                                                      @Param("soloConVariacion") boolean soloConVariacion,
                                                                                      @Param("tipoVariacion") String tipoVariacion);
}
