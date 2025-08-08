package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {

    Optional<OrdenProduccion> findByLoteProduccion(String loteProduccion);

    Long countByCodigoOrdenStartingWith(String prefijo);

}
