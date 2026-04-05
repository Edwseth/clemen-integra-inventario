package com.willyes.clemenintegra.bom.repository;

import com.willyes.clemenintegra.bom.dto.FormulaImpactoReferenciaDTO;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<DetalleFormula> findByFiltros(@Param("formulaId") Long formulaId,
                                       @Param("insumo") String insumo,
                                       Pageable pageable);

    @Query("""
            SELECT DISTINCT new com.willyes.clemenintegra.bom.dto.FormulaImpactoReferenciaDTO(
                f.id,
                CASE
                    WHEN f.versionMajor IS NOT NULL AND f.versionMinor IS NOT NULL
                        THEN CONCAT(CONCAT('V', f.versionMajor), CONCAT('.', f.versionMinor))
                    ELSE f.version
                END,
                f.estado,
                f.activo,
                p.id,
                p.codigoSku,
                p.nombre
            )
            FROM DetalleFormula d
            JOIN d.formula f
            JOIN f.producto p
            WHERE d.insumo.id = :productoId
              AND f.estado = :estadoAprobada
              AND f.activo = true
            """)
    List<FormulaImpactoReferenciaDTO> findImpactoActivasByInsumoId(@Param("productoId") Long productoId,
                                                                    @Param("estadoAprobada") EstadoFormula estadoAprobada);

    @Query("""
            SELECT DISTINCT new com.willyes.clemenintegra.bom.dto.FormulaImpactoReferenciaDTO(
                f.id,
                CASE
                    WHEN f.versionMajor IS NOT NULL AND f.versionMinor IS NOT NULL
                        THEN CONCAT(CONCAT('V', f.versionMajor), CONCAT('.', f.versionMinor))
                    ELSE f.version
                END,
                f.estado,
                f.activo,
                p.id,
                p.codigoSku,
                p.nombre
            )
            FROM DetalleFormula d
            JOIN d.formula f
            JOIN f.producto p
            WHERE d.insumo.id = :productoId
              AND NOT (f.estado = :estadoAprobada AND f.activo = true)
            """)
    List<FormulaImpactoReferenciaDTO> findImpactoHistoricasByInsumoId(@Param("productoId") Long productoId,
                                                                       @Param("estadoAprobada") EstadoFormula estadoAprobada);
}
