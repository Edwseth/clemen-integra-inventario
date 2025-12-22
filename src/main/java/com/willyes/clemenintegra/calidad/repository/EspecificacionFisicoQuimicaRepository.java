package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.EspecificacionFisicoQuimica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspecificacionFisicoQuimicaRepository extends JpaRepository<EspecificacionFisicoQuimica, Long> {
    List<EspecificacionFisicoQuimica> findByProducto_IdAndActivoTrueOrderByNombreParametroAsc(Long productoId);
}
