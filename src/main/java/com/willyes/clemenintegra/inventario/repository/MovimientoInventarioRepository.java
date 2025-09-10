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

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

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

    @Query("select coalesce(sum(m.cantidad),0) from MovimientoInventario m where m.ordenProduccion.id = :ordenId and m.producto.id = :productoId and m.tipoMovimientoDetalle.id = :detalleId")
    BigDecimal sumaCantidadPorOrdenYProducto(@Param("ordenId") Long ordenId,
                                             @Param("productoId") Long productoId,
                                             @Param("detalleId") Long detalleId);

    boolean existsByOrdenProduccionIdAndClasificacion(Long ordenProduccionId, ClasificacionMovimientoInventario clasificacion);

    boolean existsByTipoMovimientoAndProductoIdAndLoteIdAndOrdenProduccionId(
            TipoMovimiento tipoMovimiento,
            Long productoId,
            Long loteId,
            Long ordenProduccionId);

}

