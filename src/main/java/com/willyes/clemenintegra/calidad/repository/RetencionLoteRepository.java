package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetencionLoteRepository extends JpaRepository<RetencionLote, Long> {
    Page<RetencionLote> findByEstado(EstadoRetencion estado, Pageable pageable);
}
