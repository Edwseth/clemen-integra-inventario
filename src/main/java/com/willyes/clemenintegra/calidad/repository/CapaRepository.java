package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapaRepository extends JpaRepository<Capa, Long> {
    Page<Capa> findByEstado(EstadoCapa estado, Pageable pageable);

    Page<Capa> findByNoConformidad_Severidad(SeveridadNoConformidad severidad, Pageable pageable);

    Page<Capa> findByNoConformidad_SeveridadAndEstado(SeveridadNoConformidad severidad,
                                                      EstadoCapa estado, Pageable pageable);
}
