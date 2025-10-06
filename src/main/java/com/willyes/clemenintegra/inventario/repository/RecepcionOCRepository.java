package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.RecepcionOC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RecepcionOCRepository extends JpaRepository<RecepcionOC, Long> {

    Optional<RecepcionOC> findByOrdenCompraIdAndFechaRecepcion(Integer ordenCompraId, LocalDate fechaRecepcion);

    Optional<RecepcionOC> findByCodigo(String codigo);
}
