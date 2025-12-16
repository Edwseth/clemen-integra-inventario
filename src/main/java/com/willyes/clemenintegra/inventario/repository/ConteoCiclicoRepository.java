package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.ConteoCiclico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConteoCiclicoRepository extends JpaRepository<ConteoCiclico, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "almacen",
            "detalles",
            "detalles.producto",
            "detalles.loteProducto",
            "detalles.ubicacionFisica"
    })
    @Query("select distinct c from ConteoCiclico c left join c.detalles d where c.id = :id")
    Optional<ConteoCiclico> findByIdWithDetallesForUpdate(@Param("id") Long id);
}
