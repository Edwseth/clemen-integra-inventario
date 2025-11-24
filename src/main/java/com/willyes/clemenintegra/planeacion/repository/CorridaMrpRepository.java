package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CorridaMrpRepository extends JpaRepository<CorridaMrp, Long>, JpaSpecificationExecutor<CorridaMrp> {
}
