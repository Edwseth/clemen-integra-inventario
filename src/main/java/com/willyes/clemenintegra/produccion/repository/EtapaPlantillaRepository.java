package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EtapaPlantillaRepository extends JpaRepository<EtapaPlantilla, Long> {
    List<EtapaPlantilla> findByProductoIdOrderBySecuenciaAsc(Integer productoId);

    List<EtapaPlantilla> findByProductoIdAndActivoTrueOrderBySecuenciaAsc(Integer productoId);

    Optional<EtapaPlantilla> findFirstByProductoIdAndSecuencia(Integer productoId, Integer secuencia);

    Optional<EtapaPlantilla> findFirstByProductoIdAndNombreIgnoreCase(Integer productoId, String nombre);
}
