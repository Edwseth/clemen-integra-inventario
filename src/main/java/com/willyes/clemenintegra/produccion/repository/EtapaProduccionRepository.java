package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EtapaProduccionRepository extends JpaRepository<EtapaProduccion, Long> {
    List<EtapaProduccion> findByOrdenProduccionIdOrderBySecuenciaAsc(Long ordenProduccionId);

    @Query("""
            select e from EtapaProduccion e
            where e.ordenProduccion.id = :ordenId
              and e.fechaInicio is not null
              and e.fechaFin is null
            order by e.fechaInicio desc
            """)
    Optional<EtapaProduccion> findEtapaActivaByOrdenProduccionId(@Param("ordenId") Long ordenProduccionId);
}
