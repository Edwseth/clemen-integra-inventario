package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoMovimientoDetalleRepository extends JpaRepository<TipoMovimientoDetalle, Long> {
    Optional<TipoMovimientoDetalle> findByDescripcion(String descripcion);
    boolean existsByDescripcion(String descripcion);
}
