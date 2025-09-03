package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtapaPlantillaRepository extends JpaRepository<EtapaPlantilla, Long> {
    List<EtapaPlantilla> findByProductoIdAndActivoTrueOrderBySecuenciaAsc(Integer productoId);
}
