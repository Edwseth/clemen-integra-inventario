package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.calidad.model.ItemChecklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemChecklistRepository extends JpaRepository<ItemChecklist, Long> {
    Page<ItemChecklist> findByChecklist_Id(Long checklistId, Pageable pageable);
}