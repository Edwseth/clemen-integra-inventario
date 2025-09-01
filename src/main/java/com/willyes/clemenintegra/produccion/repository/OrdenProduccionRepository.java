package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long>, JpaSpecificationExecutor<OrdenProduccion> {

    Optional<OrdenProduccion> findByLoteProduccion(String loteProduccion);

    Long countByCodigoOrdenStartingWith(String prefijo);

}
