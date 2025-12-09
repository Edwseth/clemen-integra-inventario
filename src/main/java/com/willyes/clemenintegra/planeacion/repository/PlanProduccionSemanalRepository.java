package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PlanProduccionSemanalRepository extends JpaRepository<PlanProduccionSemanal, Long> {

    @Query("select p from PlanProduccionSemanal p where (:inicio is null or p.semanaInicio >= :inicio) " +
            "and (:fin is null or p.semanaInicio <= :fin) " +
            "and (:estado is null or p.estado = :estado)")
    Page<PlanProduccionSemanal> buscarPorFiltros(@Param("inicio") LocalDate inicio,
                                                 @Param("fin") LocalDate fin,
                                                 @Param("estado") EstadoPlanProduccion estado,
                                                 Pageable pageable);
}
