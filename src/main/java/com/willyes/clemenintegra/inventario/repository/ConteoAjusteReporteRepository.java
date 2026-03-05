package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.ConteoCiclicoDetalle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ConteoAjusteReporteRepository extends Repository<ConteoCiclicoDetalle, Long> {

    @Query(value = """
            SELECT cc.id AS conteoId,
                   cc.fecha_creacion AS fechaConteo,
                   cc.aplicado_en AS fechaAplicacion,
                   a.id AS almacenId,
                   a.nombre AS almacenNombre,
                   p.id AS productoId,
                   p.codigo_sku AS productoSku,
                   p.nombre AS productoNombre,
                   lp.id AS loteId,
                   lp.codigo_lote AS loteCodigo,
                   ccd.stock_sistema AS stockAntes,
                   ccd.conteo_fisico AS conteoFisico,
                   ccd.diferencia AS diferencia,
                   CASE
                       WHEN ccd.diferencia > 0 THEN 'AJUSTE_POSITIVO'
                       WHEN ccd.diferencia < 0 THEN 'AJUSTE_NEGATIVO'
                       ELSE 'SIN_AJUSTE'
                   END AS tipoAjuste,
                   (ccd.stock_sistema + ccd.diferencia) AS stockFinal,
                   u.id AS usuarioConteoId,
                   u.nombre_completo AS usuarioConteoNombre
            FROM conteos_ciclicos_detalle ccd
            JOIN conteos_ciclicos cc ON cc.id = ccd.conteo_id
            JOIN almacenes a ON a.id = cc.almacen_id
            JOIN productos p ON p.id = ccd.producto_id
            LEFT JOIN lotes_productos lp ON lp.id = ccd.lote_producto_id
            LEFT JOIN usuarios u ON u.id = cc.aplicado_por_id
            WHERE cc.aplicado_en IS NOT NULL
              AND cc.aplicado_en >= :fechaInicio
              AND cc.aplicado_en <= :fechaFin
              AND (:almacenId IS NULL OR a.id = :almacenId)
              AND (:productoId IS NULL OR p.id = :productoId)
              AND (:soloConDiferencia = FALSE OR ccd.diferencia <> 0)
            ORDER BY
              CASE WHEN :sortField = 'fechaAplicacion' AND :sortDir = 'asc' THEN cc.aplicado_en END ASC,
              CASE WHEN :sortField = 'fechaAplicacion' AND :sortDir = 'desc' THEN cc.aplicado_en END DESC,
              CASE WHEN :sortField = 'fechaConteo' AND :sortDir = 'asc' THEN cc.fecha_creacion END ASC,
              CASE WHEN :sortField = 'fechaConteo' AND :sortDir = 'desc' THEN cc.fecha_creacion END DESC,
              CASE WHEN :sortField = 'productoNombre' AND :sortDir = 'asc' THEN p.nombre END ASC,
              CASE WHEN :sortField = 'productoNombre' AND :sortDir = 'desc' THEN p.nombre END DESC,
              CASE WHEN :sortField = 'loteCodigo' AND :sortDir = 'asc' THEN lp.codigo_lote END ASC,
              CASE WHEN :sortField = 'loteCodigo' AND :sortDir = 'desc' THEN lp.codigo_lote END DESC,
              cc.id DESC, ccd.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM conteos_ciclicos_detalle ccd
            JOIN conteos_ciclicos cc ON cc.id = ccd.conteo_id
            JOIN almacenes a ON a.id = cc.almacen_id
            JOIN productos p ON p.id = ccd.producto_id
            WHERE cc.aplicado_en IS NOT NULL
              AND cc.aplicado_en >= :fechaInicio
              AND cc.aplicado_en <= :fechaFin
              AND (:almacenId IS NULL OR a.id = :almacenId)
              AND (:productoId IS NULL OR p.id = :productoId)
              AND (:soloConDiferencia = FALSE OR ccd.diferencia <> 0)
            """,
            nativeQuery = true)
    Page<ConteoAjusteReporteProjection> findReporteConteosAjuste(@Param("fechaInicio") LocalDateTime fechaInicio,
                                                                  @Param("fechaFin") LocalDateTime fechaFin,
                                                                  @Param("almacenId") Long almacenId,
                                                                  @Param("productoId") Long productoId,
                                                                  @Param("soloConDiferencia") boolean soloConDiferencia,
                                                                  @Param("sortField") String sortField,
                                                                  @Param("sortDir") String sortDir,
                                                                  Pageable pageable);
}
