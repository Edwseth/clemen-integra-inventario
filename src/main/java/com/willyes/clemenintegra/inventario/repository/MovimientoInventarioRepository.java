package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("""
    SELECT m FROM MovimientoInventario m LEFT JOIN m.motivoMovimiento mm
    WHERE (:productoId IS NULL OR m.producto.id = :productoId)
      AND (
        :almacenId IS NULL OR
        m.almacenOrigen.id = :almacenId OR
        m.almacenDestino.id = :almacenId
      )
      AND (:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento)
      AND (:clasificacion IS NULL OR mm.motivo = :clasificacion)
      AND (:fechaInicio IS NULL OR m.fechaIngreso >= :fechaInicio)
      AND (:fechaFin IS NULL OR m.fechaIngreso <= :fechaFin)
""")
    Page<MovimientoInventario> filtrarMovimientos(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("clasificacion") ClasificacionMovimientoInventario clasificacion,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            Pageable pageable
    );

    @Query("""
    SELECT m FROM MovimientoInventario m LEFT JOIN m.motivoMovimiento mm
    WHERE (:productoId IS NULL OR m.producto.id = :productoId)
      AND (
        :almacenId IS NULL OR
        m.almacenOrigen.id = :almacenId OR
        m.almacenDestino.id = :almacenId
      )
      AND (:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento)
      AND (:clasificacion IS NULL OR mm.motivo = :clasificacion)
      AND (:fechaInicio IS NULL OR m.fechaIngreso >= :fechaInicio)
      AND (:fechaFin IS NULL OR m.fechaIngreso <= :fechaFin)
    ORDER BY m.fechaIngreso DESC
""")
    List<MovimientoInventario> buscarMovimientos(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("clasificacion") ClasificacionMovimientoInventario clasificacion,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
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

}

