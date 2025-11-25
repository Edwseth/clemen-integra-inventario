package com.willyes.clemenintegra.planeacion.repository;

import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCorridaMrpRepository extends JpaRepository<DetalleCorridaMrp, Long> {

    List<DetalleCorridaMrp> findByCorridaId(Long corridaId);
}
