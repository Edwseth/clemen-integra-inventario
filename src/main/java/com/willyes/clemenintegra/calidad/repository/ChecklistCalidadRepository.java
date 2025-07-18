package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.ChecklistCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistCalidadRepository extends JpaRepository<ChecklistCalidad, Long> {
    Page<ChecklistCalidad> findByTipoChecklist(TipoChecklist tipo, Pageable pageable);
}
