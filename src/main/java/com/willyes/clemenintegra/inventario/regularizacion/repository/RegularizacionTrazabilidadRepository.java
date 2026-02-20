package com.willyes.clemenintegra.inventario.regularizacion.repository;

import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegularizacionTrazabilidadRepository extends JpaRepository<RegularizacionTrazabilidad, Long> {
    Optional<RegularizacionTrazabilidad> findByIdempotencyKey(String idempotencyKey);

    boolean existsByOrdenProduccionId(Long ordenProduccionId);
}
