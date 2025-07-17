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

    @Query("""
    SELECT p.nombre, p.codigoSku, AVG(d.valorUnitario), MAX(pr.nombre), c.tipo
    FROM OrdenCompraDetalle d
    JOIN d.producto p
    JOIN p.categoriaProducto c
    JOIN d.ordenCompra oc
    LEFT JOIN oc.proveedor pr
    WHERE (:categoria IS NULL OR c.nombre = :categoria)
    GROUP BY p.id, p.nombre, p.codigoSku, c.tipo
    ORDER BY AVG(d.valorUnitario) DESC
    """)
    java.util.List<Object[]> productosMasCostosos(@Param("categoria") String categoria);
}
