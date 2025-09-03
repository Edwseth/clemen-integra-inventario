package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtapaPlantillaRepository extends JpaRepository<EtapaPlantilla, Long> {
    List<EtapaPlantilla> findByProductoIdOrderBySecuenciaAsc(Integer productoId);

    List<EtapaPlantilla> findByProductoIdAndActivoTrueOrderBySecuenciaAsc(Integer productoId);

    boolean existsByProductoIdAndSecuencia(Integer productoId, Integer secuencia);

    boolean existsByProductoIdAndNombreIgnoreCase(Integer productoId, String nombre);

    boolean existsByProductoIdAndSecuenciaAndIdNot(Integer productoId, Integer secuencia, Long id);

    boolean existsByProductoIdAndNombreIgnoreCaseAndIdNot(Integer productoId, String nombre, Long id);
}
