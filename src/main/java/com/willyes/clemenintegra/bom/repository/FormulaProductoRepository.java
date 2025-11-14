package com.willyes.clemenintegra.bom.repository;

import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FormulaProductoRepository extends JpaRepository<FormulaProducto, Long> {
    @EntityGraph(attributePaths = "detalles")
    Optional<FormulaProducto> findByProductoId(Long productoId);

    @EntityGraph(attributePaths = "detalles")
    Optional<FormulaProducto> findByProductoIdAndEstadoAndActivoTrue(Long productoId, EstadoFormula estado);

    List<FormulaProducto> findAllByProductoId(Long productoId);

    @Query("select f from FormulaProducto f " +
            "join fetch f.producto p " +
            "left join fetch f.actualizadoPor ap " +
            "left join fetch f.creadoPor cp " +
            "order by coalesce(f.fechaActualizacion, f.fechaCreacion) desc, f.id desc")
    List<FormulaProducto> findAllForResumen();
}
