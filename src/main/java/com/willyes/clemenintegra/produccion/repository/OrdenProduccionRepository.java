package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long>, JpaSpecificationExecutor<OrdenProduccion> {

    Optional<OrdenProduccion> findByLoteProduccion(String loteProduccion);

    Long countByCodigoOrdenStartingWith(String prefijo);

    Optional<OrdenProduccion> findTopByLoteProduccionStartingWithOrderByLoteProduccionDesc(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenProduccion o where o.id = :id")
    Optional<OrdenProduccion> findByIdForUpdate(@Param("id") Long id);

}
