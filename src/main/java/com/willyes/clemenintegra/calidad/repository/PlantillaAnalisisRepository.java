package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.PlantillaAnalisis;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaAnalisisRepository extends JpaRepository<PlantillaAnalisis, Long> {
    List<PlantillaAnalisis> findByProducto_IdAndTipoAnalisisOrderByVersionDesc(Long productoId, TipoAnalisisPlantilla tipoAnalisis);

    Optional<PlantillaAnalisis> findTopByProducto_IdAndTipoAnalisisOrderByVersionDesc(Long productoId, TipoAnalisisPlantilla tipoAnalisis);

    List<PlantillaAnalisis> findByProducto_IdAndTipoAnalisisAndVigenteTrue(Long productoId, TipoAnalisisPlantilla tipoAnalisis);

    @EntityGraph(attributePaths = "campos")
    Optional<PlantillaAnalisis> findById(Long id);
}
