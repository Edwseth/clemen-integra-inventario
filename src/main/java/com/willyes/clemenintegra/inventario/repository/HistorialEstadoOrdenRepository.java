package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoOrdenRepository extends JpaRepository<HistorialEstadoOrden, Long> {
    List<HistorialEstadoOrden> findByOrdenCompra_Id(Long ordenId);
}

