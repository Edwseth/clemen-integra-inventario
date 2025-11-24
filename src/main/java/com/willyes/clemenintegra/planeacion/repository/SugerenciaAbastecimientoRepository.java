package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SugerenciaAbastecimientoRepository extends JpaRepository<SugerenciaAbastecimiento, Long>, JpaSpecificationExecutor<SugerenciaAbastecimiento> {
}
