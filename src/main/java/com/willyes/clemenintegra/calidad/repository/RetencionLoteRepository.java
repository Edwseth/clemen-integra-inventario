package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetencionLoteRepository extends JpaRepository<RetencionLote, Long> {
    Page<RetencionLote> findByEstado(EstadoRetencion estado, Pageable pageable);

    List<RetencionLote> findByLote_IdAndEstado(Long loteId, EstadoRetencion estado);

    Optional<RetencionLote> findFirstByLote_IdAndEstadoAndMotivo(Long loteId,
                                                                 EstadoRetencion estado,
                                                                 MotivoRetencion motivo);
}
