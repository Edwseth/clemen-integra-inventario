package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ControlEmpaqueLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlEmpaqueLoteRepository extends JpaRepository<ControlEmpaqueLote, Long> {
    List<ControlEmpaqueLote> findByOrdenProduccionId(Long ordenProduccionId);
    void deleteByOrdenProduccionId(Long ordenProduccionId);
}
