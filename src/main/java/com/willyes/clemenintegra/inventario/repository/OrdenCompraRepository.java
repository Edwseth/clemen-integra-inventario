package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import java.util.Set;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("SELECT o FROM OrdenCompra o " +
            "LEFT JOIN FETCH o.proveedor " +
            "LEFT JOIN FETCH o.detalles d " +
            "LEFT JOIN FETCH d.producto p " +
            "LEFT JOIN FETCH p.unidadMedida " +
            "WHERE o.id = :id")
    //@Query("SELECT o FROM OrdenCompra o LEFT JOIN FETCH o.detalles d LEFT JOIN FETCH d.producto p WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithDetalles(@Param("id") Long id);

    Page<OrdenCompra> findByEstado(EstadoOrdenCompra estado, Pageable pageable);

    Long countByCodigoOrdenStartingWith(String prefijo);

    List<OrdenCompra> findAllByOrderByIdDesc();

    @Query("""
            SELECT DISTINCT o FROM OrdenCompra o
            JOIN o.detalles d
            WHERE o.fechaCompromisoEntrega IS NOT NULL
              AND o.fechaCompromisoEntrega < CURRENT_DATE
              AND o.estado IN :estados
              AND d.cantidadRecibida < d.cantidad
            """)
    Page<OrdenCompra> findAtrasadas(Pageable pageable, @Param("estados") Set<EstadoOrdenCompra> estados);
}
