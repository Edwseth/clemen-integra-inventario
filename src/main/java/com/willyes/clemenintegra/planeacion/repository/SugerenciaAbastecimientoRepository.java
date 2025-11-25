package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SugerenciaAbastecimientoRepository extends JpaRepository<SugerenciaAbastecimiento, Long> {

    List<SugerenciaAbastecimiento> findByDetalleCorrida_Corrida_Id(Long corridaId);
}
