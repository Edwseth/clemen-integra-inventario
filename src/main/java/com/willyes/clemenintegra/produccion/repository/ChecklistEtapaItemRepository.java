package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistEtapaItemRepository extends JpaRepository<ChecklistEtapaItem, Long> {
    List<ChecklistEtapaItem> findByEtapaProduccionIdOrderByIdAsc(Long etapaId);
    void deleteByEtapaProduccionId(Long etapaId);
}
