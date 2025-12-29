package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetalleEtapaRepository extends JpaRepository<DetalleEtapa, Long> {
    Optional<DetalleEtapa> findFirstByEtapaProduccionIdAndFechaFinIsNullOrderByFechaInicioDesc(Long etapaProduccionId);
}
