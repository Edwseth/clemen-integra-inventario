package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.AjusteValorizacionLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AjusteValorizacionLoteRepository extends JpaRepository<AjusteValorizacionLote, Long> {
    Optional<AjusteValorizacionLote> findByIdempotencyKey(String idempotencyKey);
}
