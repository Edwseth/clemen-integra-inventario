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

    Optional<RegularizacionTrazabilidad> findTopByOrdenProduccionIdOrderByFechaIngresoDescIdDesc(Long ordenProduccionId);

    @Query(value = """
            WITH ult_reg AS (
              SELECT rt.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            )
            SELECT ur.id AS regularizacionId,
                   op.id AS ordenProduccionId,
                   op.cantidad_programada AS cantidadProgramada,
                   COALESCE(ur.cantidad_real, op.cantidad_programada) AS cantidadReal,
                   COALESCE(ur.diferencia, 0) AS diferencia,
                   ur.ajustar_pt AS ajustarPt,
                   ur.documento_referencia AS documentoReferencia,
                   ur.observaciones AS observaciones,
                   ur.usuario_id AS usuarioId,
                   COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) AS fechaIngreso,
                   (SELECT COUNT(*) FROM regularizacion_detalle rd WHERE rd.regularizacion_id = ur.id) AS tieneDetalle
            FROM orden_produccion op
            LEFT JOIN ult_reg ur ON ur.orden_produccion_id = op.id AND ur.rn = 1
            WHERE op.estado = 'FINALIZADA'
              AND (:fechaInicio IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) >= :fechaInicio)
              AND (:fechaFin IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR op.id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR COALESCE(ur.diferencia, 0) <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND COALESCE(ur.diferencia, 0) > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND COALESCE(ur.diferencia, 0) < 0)
                   OR (:tipoVariacion = 'CERO' AND COALESCE(ur.diferencia, 0) = 0))
            ORDER BY COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) DESC,
                     op.id DESC
            """,
            countQuery = """
            WITH ult_reg AS (
              SELECT rt.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            )
            SELECT COUNT(*)
            FROM orden_produccion op
            LEFT JOIN ult_reg ur ON ur.orden_produccion_id = op.id AND ur.rn = 1
            WHERE op.estado = 'FINALIZADA'
              AND (:fechaInicio IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) >= :fechaInicio)
              AND (:fechaFin IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR op.id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR COALESCE(ur.diferencia, 0) <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND COALESCE(ur.diferencia, 0) > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND COALESCE(ur.diferencia, 0) < 0)
                   OR (:tipoVariacion = 'CERO' AND COALESCE(ur.diferencia, 0) = 0))
            """,
            nativeQuery = true)
    Page<VariacionRegularizacionProjection> findUltimasVariacionesPorOP(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                                        @Param("fechaFin") LocalDateTime fechaFin,
                                                                        @Param("ordenProduccionId") Long ordenProduccionId,
                                                                        @Param("soloConVariacion") boolean soloConVariacion,
                                                                        @Param("tipoVariacion") String tipoVariacion,
                                                                        Pageable pageable);

    @Query(value = """
            WITH ult_reg AS (
              SELECT rt.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY rt.orden_produccion_id
                         ORDER BY rt.fecha_ingreso DESC, rt.id DESC
                     ) AS rn
              FROM regularizaciones_trazabilidad rt
            )
            SELECT ur.id AS regularizacionId,
                   op.id AS ordenProduccionId,
                   op.cantidad_programada AS cantidadProgramada,
                   COALESCE(ur.cantidad_real, op.cantidad_programada) AS cantidadReal,
                   COALESCE(ur.diferencia, 0) AS diferencia,
                   ur.ajustar_pt AS ajustarPt,
                   ur.documento_referencia AS documentoReferencia,
                   ur.observaciones AS observaciones,
                   ur.usuario_id AS usuarioId,
                   COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) AS fechaIngreso,
                   (SELECT COUNT(*) FROM regularizacion_detalle rd WHERE rd.regularizacion_id = ur.id) AS tieneDetalle
            FROM orden_produccion op
            LEFT JOIN ult_reg ur ON ur.orden_produccion_id = op.id AND ur.rn = 1
            WHERE op.estado = 'FINALIZADA'
              AND (:fechaInicio IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) >= :fechaInicio)
              AND (:fechaFin IS NULL OR COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) <= :fechaFin)
              AND (:ordenProduccionId IS NULL OR op.id = :ordenProduccionId)
              AND (:soloConVariacion = FALSE OR COALESCE(ur.diferencia, 0) <> 0)
              AND (:tipoVariacion IS NULL
                   OR (:tipoVariacion = 'POSITIVA' AND COALESCE(ur.diferencia, 0) > 0)
                   OR (:tipoVariacion = 'NEGATIVA' AND COALESCE(ur.diferencia, 0) < 0)
                   OR (:tipoVariacion = 'CERO' AND COALESCE(ur.diferencia, 0) = 0))
            ORDER BY COALESCE(ur.fecha_ingreso, op.fecha_fin, op.fecha_cierre, op.fecha_ultimo_cierre) DESC,
                     op.id DESC
            """, nativeQuery = true)
    List<VariacionRegularizacionProjection> findUltimasVariacionesPorOPSinPaginacion(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                                                      @Param("fechaFin") LocalDateTime fechaFin,
                                                                                      @Param("ordenProduccionId") Long ordenProduccionId,
                                                                                      @Param("soloConVariacion") boolean soloConVariacion,
                                                                                      @Param("tipoVariacion") String tipoVariacion);
}
