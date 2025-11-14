package com.willyes.clemenintegra.bom.repository;

import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoFormulaRepository extends JpaRepository<DocumentoFormula, Long> {
    List<DocumentoFormula> findByFormula_Id(Long formulaId);
}
