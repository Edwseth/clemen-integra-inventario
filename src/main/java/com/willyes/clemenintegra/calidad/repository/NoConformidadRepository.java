package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoConformidadRepository extends JpaRepository<NoConformidad, Long> {
    boolean existsByCodigo(String codigo);

    Page<NoConformidad> findBySeveridad(SeveridadNoConformidad severidad, Pageable pageable);

    Page<NoConformidad> findByOrigen(OrigenNoConformidad origen, Pageable pageable);

    Page<NoConformidad> findBySeveridadAndOrigen(SeveridadNoConformidad severidad,
                                                 OrigenNoConformidad origen,
                                                 Pageable pageable);

    Optional<NoConformidad> findFirstByLote_IdAndEstadoOrderByFechaRegistroDesc(Long loteId, EstadoNoConformidad estado);

    Optional<NoConformidad> findFirstByLote_IdAndEvaluacion_IdAndEstado(Long loteId,
                                                                        Long evaluacionId,
                                                                        EstadoNoConformidad estado);
}