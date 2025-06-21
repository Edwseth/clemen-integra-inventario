package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("SELECT o FROM OrdenCompra o " +
            "LEFT JOIN FETCH o.proveedor " +
            "LEFT JOIN FETCH o.detalles d " +
            "LEFT JOIN FETCH d.producto p " +
            "LEFT JOIN FETCH p.unidadMedida " +
            "WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithDetalles(@Param("id") Long id);

}
