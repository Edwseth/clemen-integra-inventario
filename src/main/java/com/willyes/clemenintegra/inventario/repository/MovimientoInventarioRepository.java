package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @EntityGraph(attributePaths = {
            "producto", "lote", "almacenOrigen", "almacenDestino", "registradoPor"
    })
    @Query("""
    select m
      from MovimientoInventario m
      where (:inicio is null or m.fechaIngreso >= :inicio)
        and (:fin    is null or m.fechaIngreso <= :fin)
        and (:productoId is null or m.producto.id = :productoId)
        and (:almacenId  is null or m.almacenOrigen.id = :almacenId or m.almacenDestino.id = :almacenId)
        and (:tipoMovimiento is null or m.tipoMovimiento = :tipoMovimiento)
        and (:clasificacion  is null or m.clasificacion = :clasificacion)
    """)
    Page<MovimientoInventario> filtrar(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("clasificacion") ClasificacionMovimientoInventario clasificacion,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "producto", "lote", "almacenOrigen", "almacenDestino", "registradoPor"
    })
    @Query("""
    select m
      from MovimientoInventario m
      where (:inicio is null or m.fechaIngreso >= :inicio)
        and (:fin    is null or m.fechaIngreso <= :fin)
        and (:productoId is null or m.producto.id = :productoId)
        and (:almacenId  is null or m.almacenOrigen.id = :almacenId or m.almacenDestino.id = :almacenId)
        and (:tipoMovimiento is null or m.tipoMovimiento = :tipoMovimiento)
        and (:clasificacion  is null or m.clasificacion = :clasificacion)
      order by m.fechaIngreso desc
    """)
    List<MovimientoInventario> buscarMovimientos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("clasificacion") ClasificacionMovimientoInventario clasificacion
    );

    @Query("""
    SELECT p.nombre, p.codigoSku, COUNT(m.id), c.tipo, u.nombre
    FROM MovimientoInventario m
    JOIN m.producto p
    JOIN p.categoriaProducto c
    JOIN p.unidadMedida u
    WHERE m.fechaIngreso >= :inicio AND m.fechaIngreso <= :fin
    GROUP BY p.id, p.nombre, p.codigoSku, c.tipo, u.nombre
    ORDER BY COUNT(m.id) DESC
    """)
    List<Object[]> conteoMovimientosDesc(@Param("inicio") java.time.LocalDateTime inicio,
                                         @Param("fin") java.time.LocalDateTime fin);

    @Query("""
    SELECT p.nombre, p.codigoSku, COUNT(m.id), c.tipo, u.nombre
    FROM MovimientoInventario m
    JOIN m.producto p
    JOIN p.categoriaProducto c
    JOIN p.unidadMedida u
    WHERE m.fechaIngreso >= :inicio AND m.fechaIngreso <= :fin
    GROUP BY p.id, p.nombre, p.codigoSku, c.tipo, u.nombre
    ORDER BY COUNT(m.id) ASC
    """)
    List<Object[]> conteoMovimientosAsc(@Param("inicio") java.time.LocalDateTime inicio,
                                        @Param("fin") java.time.LocalDateTime fin);

    boolean existsByProductoId(Long productoId);

    boolean existsBySolicitudMovimientoId(Long solicitudMovimientoId);

    Page<MovimientoInventario> findByOrdenProduccionId(Long ordenProduccionId, Pageable pageable);

    Page<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaId(Long ordenProduccionId,
                                                                               Long ordenProduccionEtapaId,
                                                                               Pageable pageable);

    Page<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacion(
            Long ordenProduccionId,
            Long ordenProduccionEtapaId,
            ClasificacionMovimientoInventario clasificacion,
            Pageable pageable);

    List<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaIdOrderByFechaIngresoAsc(
            Long ordenProduccionId,
            Long ordenProduccionEtapaId);

    List<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(
            Long ordenProduccionId,
            Long ordenProduccionEtapaId,
            ClasificacionMovimientoInventario clasificacion);

    List<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaIdOrderByFechaIngresoDesc(
            Long ordenProduccionId,
            Long ordenProduccionEtapaId);

    List<MovimientoInventario> findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoDesc(
            Long ordenProduccionId,
            Long ordenProduccionEtapaId,
            ClasificacionMovimientoInventario clasificacion);

    @EntityGraph(attributePaths = {
            "producto", "lote", "almacenOrigen", "almacenDestino", "registradoPor"
    })
    @Query("""
            select m
              from MovimientoInventario m
             where (:codigoRecepcion is null or :codigoRecepcion = '' or lower(m.codigoRecepcion) like lower(concat('%', :codigoRecepcion, '%')))
               and (:tipoMovimiento is null or m.tipoMovimiento = :tipoMovimiento)
            """)
    Page<MovimientoInventario> findAllByCodigoRecepcionAndTipoMovimiento(@Nullable String codigoRecepcion,
                                                                         @Nullable TipoMovimiento tipoMovimiento,
                                                                         Pageable pageable);

    Page<MovimientoInventario> findByOrdenProduccionIdAndClasificacion(Long ordenProduccionId,
                                                                       ClasificacionMovimientoInventario clasificacion,
                                                                       Pageable pageable);

    @Query("select coalesce(sum(m.cantidad),0) from MovimientoInventario m where m.ordenProduccion.id = :ordenId and m.producto.id = :productoId and m.tipoMovimientoDetalle.id = :detalleId")
    BigDecimal sumaCantidadPorOrdenYProducto(@Param("ordenId") Long ordenId,
                                             @Param("productoId") Long productoId,
                                             @Param("detalleId") Long detalleId);

    @Query("select coalesce(sum(m.cantidad),0) from MovimientoInventario m " +
            "where m.ordenProduccion.id = :ordenId " +
            "and m.producto.id = :productoId " +
            "and m.clasificacion = :clasificacion " +
            "and m.tipoMovimiento = :tipoMovimiento")
    BigDecimal sumaCantidadPorOrdenProductoClasificacion(@Param("ordenId") Long ordenId,
                                                         @Param("productoId") Long productoId,
                                                         @Param("clasificacion") ClasificacionMovimientoInventario clasificacion,
                                                         @Param("tipoMovimiento") TipoMovimiento tipoMovimiento);

    @EntityGraph(attributePaths = {
            "producto", "producto.unidadMedida", "lote", "almacenOrigen", "almacenDestino",
            "proveedor", "ordenCompra", "motivoMovimiento", "tipoMovimientoDetalle", "registradoPor"
    })
    @Query("""
        select m
          from MovimientoInventario m
         where (:inicio is null or m.fechaIngreso >= :inicio)
           and (:fin    is null or m.fechaIngreso <= :fin)
    """)
    List<MovimientoInventario> findAllForReporte(@Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin);

    boolean existsByOrdenProduccionIdAndClasificacion(Long ordenProduccionId, ClasificacionMovimientoInventario clasificacion);

    java.util.Optional<MovimientoInventario> findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
            TipoMovimiento tipoMovimiento,
            Long motivoMovimientoId,
            Long ordenProduccionId,
            Long productoId,
            Long loteId);

    boolean existsByTipoMovimientoAndProductoIdAndLoteIdAndOrdenProduccionId(
            TipoMovimiento tipoMovimiento,
            Long productoId,
            Long loteId,
            Long ordenProduccionId);

    boolean existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
            TipoMovimiento tipoMovimiento,
            Long loteId,
            Long almacenOrigenId,
            Long almacenDestinoId,
            ClasificacionMovimientoInventario clasificacion);

    @EntityGraph(attributePaths = {
            "producto",
            "lote",
            "almacenOrigen",
            "almacenDestino",
            "registradoPor",
            "solicitudMovimiento"
    })
    List<MovimientoInventario> findAllByCodigoRecepcionOrderByFechaIngresoAsc(String codigoRecepcion);

    @EntityGraph(attributePaths = {
            "producto",
            "lote",
            "almacenOrigen",
            "almacenDestino",
            "registradoPor",
            "solicitudMovimiento"
    })
    List<MovimientoInventario> findAllByRecepcionOcIdOrderByFechaIngresoAsc(Long recepcionOcId);

    @EntityGraph(attributePaths = {
            "almacenOrigen",
            "almacenDestino",
            "motivoMovimiento",
            "registradoPor",
            "ordenProduccion"
    })
    List<MovimientoInventario> findByLote_IdOrderByFechaIngresoDesc(Long loteId);

    @Query("select coalesce(sum(m.cantidad),0) from MovimientoInventario m " +
            "where m.solicitudMovimiento.id = :solicitudId " +
            "and m.producto.id = :productoId " +
            "and m.lote.id = :loteId " +
            "and m.tipoMovimiento = :tipoMov " +
            "and (:tipoDetalleId is null or m.tipoMovimientoDetalle.id = :tipoDetalleId) " +
            "and (:motivoId is null or m.motivoMovimiento.id = :motivoId)")
    BigDecimal sumaPorSolicitudYTipo(@Param("solicitudId") Long solicitudId,
                                     @Param("productoId") Long productoId,
                                     @Param("loteId") Long loteId,
                                     @Param("tipoMov") TipoMovimiento tipoMov,
                                     @Param("tipoDetalleId") Long tipoDetalleId,
                                     @Param("motivoId") Long motivoId);

    boolean existsByLoteIdAndMotivoMovimientoIdAndFechaIngresoBetween(Long loteId,
                                                                      Long motivoMovimientoId,
                                                                      LocalDateTime fechaInicio,
                                                                      LocalDateTime fechaFin);

    Optional<MovimientoInventario> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {
            "producto",
            "lote",
            "almacenOrigen",
            "almacenDestino",
            "registradoPor",
            "motivoMovimiento",
            "ordenProduccion"
    })
    @Query("""
        select m
          from MovimientoInventario m
         where (:productoId is null or m.producto.id = :productoId)
           and (:loteId is null or m.lote.id = :loteId)
           and (:inicio is null or m.fechaIngreso >= :inicio)
           and (:fin is null or m.fechaIngreso <= :fin)
           and (:almacenId is null or m.almacenOrigen.id = :almacenId or m.almacenDestino.id = :almacenId)
         order by m.fechaIngreso asc, m.id asc
    """)
    List<MovimientoInventario> buscarParaKardex(@Param("inicio") LocalDateTime inicio,
                                                @Param("fin") LocalDateTime fin,
                                                @Param("productoId") Long productoId,
                                                @Param("loteId") Long loteId,
                                                @Param("almacenId") Long almacenId);

}
