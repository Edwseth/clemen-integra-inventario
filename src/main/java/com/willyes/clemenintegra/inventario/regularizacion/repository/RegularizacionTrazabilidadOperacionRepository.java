package com.willyes.clemenintegra.inventario.regularizacion.repository;

import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegularizacionTrazabilidadOperacionRepository extends JpaRepository<RegularizacionTrazabilidadOperacion, Long> {
    Optional<RegularizacionTrazabilidadOperacion> findByIdempotencyKey(String key);
}
