package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenCompraDetalleRepository extends JpaRepository<OrdenCompraDetalle, Long> {
    void deleteByOrdenCompra_Id(Long ordenCompraId);

    @Query("SELECT o FROM OrdenCompra o " +
            "LEFT JOIN FETCH o.proveedor " +
            "LEFT JOIN FETCH o.detalles d " +
            "LEFT JOIN FETCH d.producto p " +
            "LEFT JOIN FETCH p.unidadMedida " +
            "WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithDetalles(@Param("id") Long id);
}
