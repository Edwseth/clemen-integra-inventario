package com.willyes.clemenintegra.inventario.domain.repository;

import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.domain.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("""
        SELECT m FROM MovimientoInventario m
        WHERE (:productoId IS NULL OR m.producto.id = :productoId)
          AND (:almacenId IS NULL OR m.almacen.id = :almacenId)
          AND (:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento)
          AND (:fechaInicio IS NULL OR m.fechaIngreso >= :fechaInicio)
          AND (:fechaFin IS NULL OR m.fechaIngreso <= :fechaFin)
    """)
    Page<MovimientoInventario> filtrarMovimientos(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            Pageable pageable
    );
}

