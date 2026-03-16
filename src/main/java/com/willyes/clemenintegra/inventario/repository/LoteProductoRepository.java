package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.dto.LoteAlertaActivaProjection;
import com.willyes.clemenintegra.inventario.dto.StockDisponibleListadoProjection;
import com.willyes.clemenintegra.inventario.dto.LoteUbicacionPicklistProjection;
import com.willyes.clemenintegra.inventario.dto.StockAlertaProjection;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long>, JpaSpecificationExecutor<LoteProducto> {

    Optional<LoteProducto> findByCodigoLoteAndProductoId(String codigoLote, Long productoId);
    Optional<LoteProducto> findByOrdenProduccionIdAndProductoId(Long ordenProduccionId, Long productoId);
    boolean existsByProducto(Producto producto);
    boolean existsByCodigoLote(String codigoLote);
    List<LoteProducto> findByEstado(EstadoLote estado);
    @Query("""
SELECT lp
FROM LoteProducto lp
WHERE lp.codigoLote = :codigoLote
  AND lp.ordenProduccion.id = :ordenProduccionId
""")
    List<LoteProducto> findByCodigoLoteAndOrdenProduccion(@Param("codigoLote") String codigoLote,
                                                          @Param("ordenProduccionId") Long ordenProduccionId);
    @Query("""
       SELECT lp
         FROM LoteProducto lp
         JOIN FETCH lp.producto p
         JOIN FETCH lp.almacen a
        WHERE lp.fechaVencimiento BETWEEN :inicio AND :fin
    """)
    List<LoteProducto> findByFechaVencimientoBetweenFetchProducto(@Param("inicio") LocalDateTime inicio,
                                                                  @Param("fin") LocalDateTime fin);
    @Query("""
       SELECT lp
         FROM LoteProducto lp
         JOIN FETCH lp.producto p
         JOIN FETCH lp.almacen a
        WHERE lp.fechaVencimiento BETWEEN :inicio AND :fin
          AND lp.estado IN :estados
    """)
    List<LoteProducto> findAlertasProximasVencer(@Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin,
                                                 @Param("estados") Collection<EstadoLote> estados);
    @Query("""
       SELECT lp
         FROM LoteProducto lp
         JOIN FETCH lp.producto p
         JOIN FETCH lp.almacen a
        WHERE lp.fechaVencimiento < :corte
          AND (:productoId IS NULL OR p.id = :productoId)
          AND (:almacenId  IS NULL OR a.id = :almacenId)
    """)
    List<LoteProducto> findVencidosFetch(@Param("corte") LocalDateTime corte,
                                         @Param("productoId") Long productoId,
                                         @Param("almacenId") Long almacenId);
    @Query("""
       SELECT lp
         FROM LoteProducto lp
         JOIN FETCH lp.producto p
         JOIN FETCH lp.almacen a
        WHERE lp.fechaVencimiento < :corte
          AND lp.estado IN :estados
    """)
    List<LoteProducto> findAlertasVencidos(@Param("corte") LocalDateTime corte,
                                           @Param("estados") Collection<EstadoLote> estados);
    Optional<LoteProducto> findByCodigoLoteAndProductoIdAndAlmacenId(String codigoLote, Integer productoId, Integer almacenId);
    List<LoteProducto> findByEstadoIn(List<EstadoLote> estados);
    @Query("""
       SELECT lp
         FROM LoteProducto lp
         JOIN FETCH lp.producto p
         JOIN FETCH lp.almacen a
        WHERE lp.estado IN :estados
    """)
    List<LoteProducto> findAlertasPendientesLiberar(@Param("estados") Collection<EstadoLote> estados);
    List<LoteProducto> findByEstadoInAndProducto_TipoAnalisisIn(List<EstadoLote> estados, List<TipoAnalisisCalidad> tipos);
    List<LoteProducto> findByFechaVencimientoBeforeAndEstadoNotIn(LocalDateTime fecha,
                                                                  List<EstadoLote> estados);
    Optional<LoteProducto> findFirstByProductoIdAndEstadoAndStockLoteGreaterThanOrderByFechaVencimientoAsc(
            Integer productoId,
            EstadoLote estado,
            BigDecimal stockLote
    );

    @Query("SELECT lp.estado, COALESCE(SUM(lp.stockLote - lp.stockReservado), 0) " +
            "FROM LoteProducto lp " +
            "WHERE lp.producto.id = :productoId AND (lp.stockLote - lp.stockReservado) > 0 " +
            "GROUP BY lp.estado")
    List<Object[]> sumarStockPorEstado(@Param("productoId") Long productoId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteProducto l where l.id = :id")
    Optional<LoteProducto> findByIdForUpdate(@Param("id") Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteProducto l where l.id = :id")
    Optional<LoteProducto> findByIdWithLock(@Param("id") Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteProducto l where l.producto.id = :productoId and l.codigoLote = :codigoLote and l.almacen.id = :almacenId")
    Optional<LoteProducto> findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(@Param("productoId") Integer productoId,
                                                                              @Param("codigoLote") String codigoLote,
                                                                              @Param("almacenId") Integer almacenId);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<LoteProducto> findByProductoIdAndAlmacenIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
            Long productoId,
            Integer almacenId,
            Collection<EstadoLote> estados);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<LoteProducto> findByProductoIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
            Long productoId,
            Collection<EstadoLote> estados);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<LoteProducto> findByProductoIdAndEstadoIn(Long productoId, Collection<EstadoLote> estados);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<LoteProducto> findByProductoIdAndAlmacenIdAndEstadoIn(Long productoId,
                                                                Integer almacenId,
                                                                Collection<EstadoLote> estados);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<LoteProducto> findByOrdenProduccionId(Long ordenProduccionId);

    @EntityGraph(attributePaths = {"producto"})
    @Query("""
       SELECT lp
         FROM LoteProducto lp
        WHERE lp.fechaFabricacion BETWEEN :inicio AND :fin
          AND lp.estado IN :estados
          AND (:estadoLote IS NULL OR lp.estado = :estadoLote)
    """)
    org.springframework.data.domain.Page<LoteProducto> findConsolidadoCalidad(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") Collection<EstadoLote> estados,
            @Param("estadoLote") EstadoLote estadoLote,
            org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {
            "almacen",
            "producto",
            "producto.plantillaAnalisisMicrobiologico",
            "usuarioLiberador",
            "lotePsOrigen",
            "ubicacionFisica",
            "ordenProduccion"
    })
    org.springframework.data.domain.Page<LoteProducto> findAll(org.springframework.data.jpa.domain.Specification<LoteProducto> spec,
                                                              org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {
            "almacen",
            "producto",
            "producto.plantillaAnalisisMicrobiologico",
            "usuarioLiberador",
            "lotePsOrigen",
            "ubicacionFisica",
            "ordenProduccion"
    })
    List<LoteProducto> findAll(Specification<LoteProducto> spec, Sort sort);

    @Query("""
      select l
      from LoteProducto l
      where l.producto.id = :productoId
        and l.almacen.id = :almacenId
        and (l.agotado = false or l.agotado is null)
        and l.estado in :estados
      order by l.fechaVencimiento asc nulls last, l.id asc
    """)
        List<LoteProducto> findFefoSalidaPt(@Param("productoId") Long productoId,
                                            @Param("almacenId") Long almacenId,
                                            @Param("estados") Set<EstadoLote> estados);


    @Query(value = """
        SELECT lp.id AS loteProductoId,
               lp.codigo_lote AS codigoLote,
               (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) AS stockLote,
               lp.stock_lote AS stockFisico,
               COALESCE(lp.stock_reservado, 0) AS stockReservado,
               lp.fecha_vencimiento AS fechaVencimiento,
               lp.almacenes_id AS almacenId,
               a.nombre AS nombreAlmacen,
               lp.estado AS estado
        FROM lotes_productos lp
        LEFT JOIN almacenes a ON lp.almacenes_id = a.id
        WHERE lp.productos_id = :productoId
          AND lp.estado IN ('DISPONIBLE','LIBERADO')
          AND lp.agotado = false
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
        ORDER BY lp.fecha_vencimiento ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection> findFefoDisponibles(
            @Param("productoId") Long productoId, @Param("limit") int limit);

    // LÍNEA CODEx: nuevas consultas para disponibilidad detallada por producto
    @Query("""
      select lp.estado as estado, coalesce(sum(lp.stockLote - lp.stockReservado),0)
      from LoteProducto lp
      where lp.producto.id = :productoId and (lp.stockLote - lp.stockReservado) > 0 and lp.agotado = false
      group by lp.estado
    """)
    List<Object[]> sumarPorEstado(@Param("productoId") Long productoId);

    @Query("""
     select new com.willyes.clemenintegra.bom.dto.LoteResumenDTO(
        lp.id, lp.codigoLote, lp.estado, a.nombre, (lp.stockLote - lp.stockReservado),
        lp.fechaVencimiento, lp.fechaLiberacion, u.nombreCompleto
     )
     from LoteProducto lp
     left join lp.almacen a
     left join lp.usuarioLiberador u
     where lp.producto.id = :productoId and (lp.stockLote - lp.stockReservado) > 0 and lp.agotado = false
     order by lp.estado asc, lp.fechaVencimiento asc
    """)
    List<com.willyes.clemenintegra.bom.dto.LoteResumenDTO> listarLotesPorProducto(@Param("productoId") Long productoId);


    @Query("""
        select lp
        from LoteProducto lp
        where lp.producto.id = :productoId
          and lp.almacen.id = :almacenId
          and lp.agotado = false
          and (lp.stockLote - coalesce(lp.stockReservado, 0)) > 0
        order by lp.fechaVencimiento asc nulls last, lp.id asc
    """)
    List<LoteProducto> findFefoByProductoAndAlmacen(@Param("productoId") Long productoId,
                                                    @Param("almacenId") Integer almacenId);

    List<LoteProducto> findAllByCodigoLoteAndProductoId(String codigoLote, Long productoId);

    @Query("""
        select l.id
        from LoteProducto l
        where l.fechaVencimiento is not null
          and l.fechaVencimiento < :cutoff
          and l.estado not in :estadosExcluidos
        order by l.id asc
    """)
    List<Long> findIdsParaExpirar(@Param("cutoff") LocalDateTime cutoff,
                                  @Param("estadosExcluidos") Collection<EstadoLote> estadosExcluidos,
                                  org.springframework.data.domain.Pageable pageable);

    @Query("""
        select coalesce(sum(lp.stockLote), 0)
        from LoteProducto lp
        where lp.producto.id = :productoId
          and lp.almacen.id = :almacenId
          and (:ubicacionId is null or lp.ubicacionFisica.id = :ubicacionId)
    """)
    BigDecimal sumarStockPorProductoYAlmacen(@Param("productoId") Long productoId,
                                             @Param("almacenId") Integer almacenId,
                                             @Param("ubicacionId") Long ubicacionId);

    @Query("""
        select lp
        from LoteProducto lp
        left join fetch lp.ubicacionFisica uf
        where lp.producto.id = :productoId
          and lp.almacen.id = :almacenId
          and lp.estado in :estados
          and (:ubicacionId is null or uf.id = :ubicacionId)
          and (:q is null or lower(lp.codigoLote) like lower(concat('%', :q, '%')))
        order by lp.fechaVencimiento asc nulls last, lp.codigoLote asc
    """)
    List<LoteProducto> buscarParaConteo(@Param("productoId") Long productoId,
                                        @Param("almacenId") Integer almacenId,
                                        @Param("ubicacionId") Long ubicacionId,
                                        @Param("q") String q,
                                        @Param("estados") Collection<EstadoLote> estados);

    @Query("""
        select lp
        from LoteProducto lp
        left join fetch lp.ubicacionFisica uf
        where lp.producto.id = :productoId
          and lp.almacen.id = :almacenId
        order by lp.fechaVencimiento asc nulls last, lp.id asc
    """)
    List<LoteProducto> buscarParaConteoPorProductoYAlmacen(@Param("productoId") Long productoId,
                                                            @Param("almacenId") Integer almacenId);

    @Query(value = """
        SELECT lp.productos_id        AS productoId,
               p.nombre               AS nombreProducto,
               p.codigo_sku           AS codigoSku,
               lp.almacenes_id        AS almacenId,
               a.nombre               AS nombreAlmacen,
               p.stock_minimo         AS stockMinimo,
               p.stock_maximo_planeacion AS stockMaximoPlaneacion,
               COALESCE(SUM(GREATEST(lp.stock_lote - COALESCE(lp.stock_reservado, 0), 0)), 0) AS stockActual
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
                 JOIN almacenes a ON a.id = lp.almacenes_id
        GROUP BY lp.productos_id,
                 p.nombre,
                 p.codigo_sku,
                 lp.almacenes_id,
                 a.nombre,
                 p.stock_minimo,
                 p.stock_maximo_planeacion
        """, nativeQuery = true)
    List<StockAlertaProjection> sumarStockParaAlertas();

    @Query(value = """
        SELECT lp.id                AS loteProductoId,
               lp.codigo_lote       AS codigoLote,
               lp.fecha_vencimiento AS fechaVencimiento,
               lp.productos_id      AS productoId,
               p.nombre             AS nombreProducto,
               p.codigo_sku         AS codigoSku,
               lp.almacenes_id      AS almacenId,
               a.nombre             AS nombreAlmacen,
               GREATEST(lp.stock_lote - COALESCE(lp.stock_reservado, 0), 0) AS stockActual
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
                 JOIN almacenes a ON a.id = lp.almacenes_id
        WHERE lp.fecha_vencimiento IS NOT NULL
        """, nativeQuery = true)
    List<LoteAlertaActivaProjection> listarLotesConVencimiento();

    @Query(value = """
        SELECT lp.productos_id AS productoId,
               lp.almacenes_id AS almacenId,
               lp.codigo_lote AS codigoLote,
               uf.codigo AS ubicacionCodigo,
               uf.descripcion AS ubicacionDescripcion
        FROM lotes_productos lp
        LEFT JOIN ubicaciones_fisicas uf ON uf.id = lp.ubicaciones_fisicas_id
        WHERE lp.productos_id IN (:productoIds)
          AND lp.almacenes_id IN (:almacenIds)
          AND lp.codigo_lote IN (:codigosLote)
        """, nativeQuery = true)
    List<LoteUbicacionPicklistProjection> findUbicacionesFisicasPicklist(
            @Param("productoIds") Collection<Integer> productoIds,
            @Param("almacenIds") Collection<Integer> almacenIds,
            @Param("codigosLote") Collection<String> codigosLote);


    @Query(value = """
        SELECT lp.id AS loteId,
               lp.codigo_lote AS codigoLote,
               lp.productos_id AS productoId,
               p.nombre AS nombreProducto,
               (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) AS stockDisponible,
               lp.fecha_vencimiento AS fechaVencimiento,
               lp.estado AS estado,
               lp.almacenes_id AS almacenIdActual,
               a.nombre AS nombreAlmacenActual
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
                 JOIN categorias_producto cp ON cp.id = p.categorias_producto_id
                 JOIN almacenes a ON a.id = lp.almacenes_id
        WHERE lp.estado = 'LIBERADO'
          AND lp.almacenes_id = :almacenCuarentenaId
          AND cp.tipo = 'PRODUCTO_TERMINADO'
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
        ORDER BY (lp.fecha_vencimiento IS NULL) ASC, lp.fecha_vencimiento ASC, lp.id ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
                 JOIN categorias_producto cp ON cp.id = p.categorias_producto_id
        WHERE lp.estado = 'LIBERADO'
          AND lp.almacenes_id = :almacenCuarentenaId
          AND cp.tipo = 'PRODUCTO_TERMINADO'
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
        """,
            nativeQuery = true)
    org.springframework.data.domain.Page<com.willyes.clemenintegra.inventario.dto.LotePendienteUbicarPtProjection> findPendientesUbicarPt(
            @Param("almacenCuarentenaId") Long almacenCuarentenaId,
            org.springframework.data.domain.Pageable pageable);


    @Query(value = """
        SELECT DISTINCT lp.id AS loteId,
               lp.codigo_lote AS codigoLote,
               p.id AS productoId,
               p.nombre AS nombreProducto,
               cp.tipo AS tipoProducto,
               (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) AS stockDisponible,
               lp.fecha_vencimiento AS fechaVencimiento,
               lp.estado AS estado,
               lp.almacenes_id AS almacenIdActual,
               a.nombre AS nombreAlmacenActual,
               ad.id AS almacenDestinoSugeridoId,
               ad.nombre AS nombreAlmacenDestinoSugerido
        FROM lotes_productos lp
                 JOIN productos p ON lp.productos_id = p.id
                 JOIN categorias_producto cp ON p.categorias_producto_id = cp.id
                 JOIN almacenes a ON lp.almacenes_id = a.id
                 LEFT JOIN almacenes ad ON ad.id = (
                     SELECT MIN(ad2.id)
                     FROM almacenes ad2
                     WHERE ad2.categoria_almacen = cp.tipo
                       AND ad2.nombre LIKE 'Principal%'
                 )
        WHERE lp.estado = 'LIBERADO'
          AND lp.almacenes_id = :almacenCuarentenaId
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
        ORDER BY (lp.fecha_vencimiento IS NULL) ASC, lp.fecha_vencimiento ASC, lp.id ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM lotes_productos lp
                 JOIN productos p ON lp.productos_id = p.id
                 JOIN categorias_producto cp ON p.categorias_producto_id = cp.id
        WHERE lp.estado = 'LIBERADO'
          AND lp.almacenes_id = :almacenCuarentenaId
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
        """,
            nativeQuery = true)
    org.springframework.data.domain.Page<com.willyes.clemenintegra.inventario.dto.LotePendienteUbicarProjection> findPendientesUbicar(
            @Param("almacenCuarentenaId") Long almacenCuarentenaId,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
        SELECT p.codigo_sku AS sku,
               p.nombre AS nombre,
               cp.nombre AS categoria,
               (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) AS cantidadActual,
               lp.codigo_lote AS lote,
               lp.fecha_vencimiento AS fechaVencimiento,
               a.nombre AS almacenNombre,
               uf.codigo AS ubicacionCodigo,
               uf.descripcion AS ubicacionDescripcion
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
                 LEFT JOIN categorias_producto cp ON cp.id = p.categorias_producto_id
                 LEFT JOIN almacenes a ON a.id = lp.almacenes_id
                 LEFT JOIN ubicaciones_fisicas uf ON uf.id = lp.ubicaciones_fisicas_id
        WHERE lp.estado IN ('DISPONIBLE','LIBERADO')
          AND lp.agotado = false
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
          AND (:categoriaId IS NULL OR p.categorias_producto_id = :categoriaId)
          AND (
                :q IS NULL OR :q = ''
                OR UPPER(p.nombre) LIKE CONCAT('%', UPPER(:q), '%')
                OR UPPER(p.codigo_sku) LIKE CONCAT('%', UPPER(:q), '%')
                OR UPPER(lp.codigo_lote) LIKE CONCAT('%', UPPER(:q), '%')
          )
        ORDER BY p.nombre ASC, lp.codigo_lote ASC, lp.fecha_vencimiento ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM lotes_productos lp
                 JOIN productos p ON p.id = lp.productos_id
        WHERE lp.estado IN ('DISPONIBLE','LIBERADO')
          AND lp.agotado = false
          AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
          AND (:categoriaId IS NULL OR p.categorias_producto_id = :categoriaId)
          AND (
                :q IS NULL OR :q = ''
                OR UPPER(p.nombre) LIKE CONCAT('%', UPPER(:q), '%')
                OR UPPER(p.codigo_sku) LIKE CONCAT('%', UPPER(:q), '%')
                OR UPPER(lp.codigo_lote) LIKE CONCAT('%', UPPER(:q), '%')
          )
        """,
            nativeQuery = true)
    org.springframework.data.domain.Page<StockDisponibleListadoProjection> findStockDisponibleListado(
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            org.springframework.data.domain.Pageable pageable);

}
