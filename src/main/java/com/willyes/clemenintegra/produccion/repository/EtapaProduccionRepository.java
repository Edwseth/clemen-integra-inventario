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
              and (
                    e.estado = 'EN_PROCESO'
                    or (e.fechaInicio is not null and e.fechaFin is null)
              )
            order by
                case when e.estado = 'EN_PROCESO' then 0 else 1 end,
                e.fechaInicio desc
            """)
    List<EtapaProduccion> findEtapasActivasByOrdenProduccionId(@Param("ordenId") Long ordenProduccionId);
}
