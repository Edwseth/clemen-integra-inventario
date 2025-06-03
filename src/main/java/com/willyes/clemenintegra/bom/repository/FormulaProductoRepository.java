package com.willyes.clemenintegra.bom.repository;

import com.willyes.clemenintegra.bom.model.FormulaProducto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormulaProductoRepository extends JpaRepository<FormulaProducto, Long> {
    @EntityGraph(attributePaths = "detalles")
    Optional<FormulaProducto> findByProductoId(Long productoId);

}
