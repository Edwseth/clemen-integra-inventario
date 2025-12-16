package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.ConteoCiclico;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConteoCiclicoRepository extends JpaRepository<ConteoCiclico, Long> {

    @EntityGraph(attributePaths = {
            "almacen",
            "detalles",
            "detalles.producto",
            "detalles.loteProducto",
            "detalles.ubicacionFisica"
    })
    @Query("select distinct c from ConteoCiclico c left join c.detalles d where c.id = :id")
    Optional<ConteoCiclico> findByIdWithDetalles(@Param("id") Long id);

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

    @Query("select c from ConteoCiclico c where (:almacenId is null or c.almacen.id = :almacenId) " +
            "and (:estado is null or c.estado = :estado)")
    Page<ConteoCiclico> buscar(@Param("almacenId") Integer almacenId,
                               @Param("estado") EstadoConteoCiclico estado,
                               Pageable pageable);
}
