package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.ConteoCiclicoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConteoCiclicoDetalleRepository extends JpaRepository<ConteoCiclicoDetalle, Long> {
    List<ConteoCiclicoDetalle> findByConteoId(Long conteoId);
}
