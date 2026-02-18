package com.willyes.clemenintegra.bom.repository;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleFormulaRepository extends JpaRepository<DetalleFormula, Long> {

    @Query("""
            SELECT d
            FROM DetalleFormula d
            JOIN d.insumo i
            WHERE (:formulaId IS NULL OR d.formula.id = :formulaId)
              AND (:insumo IS NULL OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :insumo, '%')))
            """)
    List<DetalleFormula> findByFiltros(@Param("formulaId") Long formulaId, @Param("insumo") String insumo);
}
