package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EtapaProduccionRepository extends JpaRepository<EtapaProduccion, Long> {
    List<EtapaProduccion> findByOrdenProduccionIdOrderBySecuenciaAsc(Long ordenProduccionId);

    Optional<EtapaProduccion> findTopByOrdenProduccionIdAndFechaFinIsNullOrderByFechaInicioDesc(Long ordenProduccionId);
}
