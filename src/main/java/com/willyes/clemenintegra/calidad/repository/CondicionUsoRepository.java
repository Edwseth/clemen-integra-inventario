package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.CondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CondicionUsoRepository extends JpaRepository<CondicionUso, Long> {

    List<CondicionUso> findByLote_IdAndEstado(Long loteId, EstadoCondicionUso estado);
    List<CondicionUso> findByLote_Id(Long loteId);
    Optional<CondicionUso> findFirstByLote_IdAndEstadoAndTipo(Long loteId,
                                                              EstadoCondicionUso estado,
                                                              TipoCondicionUso tipo);
}
