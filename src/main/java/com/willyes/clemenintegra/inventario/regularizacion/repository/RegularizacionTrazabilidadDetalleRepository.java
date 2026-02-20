package com.willyes.clemenintegra.inventario.regularizacion.repository;

import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegularizacionTrazabilidadDetalleRepository extends JpaRepository<RegularizacionTrazabilidadDetalle, Long> {
    List<RegularizacionTrazabilidadDetalle> findByRegularizacionIdOrderByIdAsc(Long regularizacionId);
}
