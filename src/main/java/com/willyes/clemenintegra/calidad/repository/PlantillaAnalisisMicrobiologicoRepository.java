package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaAnalisisMicrobiologicoRepository extends JpaRepository<PlantillaAnalisisMicrobiologico, Long> {
    List<PlantillaAnalisisMicrobiologico> findByProducto_IdOrderByVersionDesc(Long productoId);

    Optional<PlantillaAnalisisMicrobiologico> findTopByProducto_IdOrderByVersionDesc(Long productoId);

    @EntityGraph(attributePaths = {"producto", "parametros"})
    List<PlantillaAnalisisMicrobiologico> findByVigenteTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "parametros")
    Optional<PlantillaAnalisisMicrobiologico> findById(Long id);
}
