package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
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

    @Query(value = """
            select new com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO(
                o.id,
                o.codigoOrden,
                o.estado,
                p.nombre,
                o.fechaOrden,
                o.fechaCompromisoEntrega,
                o.descuento,
                coalesce(sum(d.cantidad), 0),
                coalesce(sum(d.cantidadRecibida), 0)
            )
            from OrdenCompra o
            join o.proveedor p
            left join o.detalles d
            group by o.id, o.codigoOrden, o.estado, p.nombre, o.fechaOrden, o.fechaCompromisoEntrega, o.descuento
            """,
            countQuery = "select count(o) from OrdenCompra o")
    Page<OrdenCompraResponseDTO> findListado(Pageable pageable);

    @Query(value = """
            select new com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO(
                o.id,
                o.codigoOrden,
                o.estado,
                p.nombre,
                o.fechaOrden,
                o.fechaCompromisoEntrega,
                o.descuento,
                coalesce(sum(d.cantidad), 0),
                coalesce(sum(d.cantidadRecibida), 0)
            )
            from OrdenCompra o
            join o.proveedor p
            left join o.detalles d
            where o.estado = :estado
            group by o.id, o.codigoOrden, o.estado, p.nombre, o.fechaOrden, o.fechaCompromisoEntrega, o.descuento
            """,
            countQuery = "select count(o) from OrdenCompra o where o.estado = :estado")
    Page<OrdenCompraResponseDTO> findListadoPorEstado(@Param("estado") EstadoOrdenCompra estado, Pageable pageable);

    @Query(value = """
            select new com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO(
                o.id,
                o.codigoOrden,
                o.estado,
                p.nombre,
                o.fechaOrden,
                o.fechaCompromisoEntrega,
                o.descuento,
                coalesce(sum(d.cantidad), 0),
                coalesce(sum(d.cantidadRecibida), 0)
            )
            from OrdenCompra o
            join o.proveedor p
            left join o.detalles d
            where o.fechaCompromisoEntrega is not null
              and o.fechaCompromisoEntrega < current_date
              and o.estado in :estados
              and exists (
                  select 1 from OrdenCompraDetalle d1
                  where d1.ordenCompra = o and d1.cantidadRecibida < d1.cantidad
              )
            group by o.id, o.codigoOrden, o.estado, p.nombre, o.fechaOrden, o.fechaCompromisoEntrega, o.descuento
            """,
            countQuery = """
            select count(o) from OrdenCompra o
            where o.fechaCompromisoEntrega is not null
              and o.fechaCompromisoEntrega < current_date
              and o.estado in :estados
              and exists (
                  select 1 from OrdenCompraDetalle d1
                  where d1.ordenCompra = o and d1.cantidadRecibida < d1.cantidad
              )
            """)
    Page<OrdenCompraResponseDTO> findListadoAtrasadas(Pageable pageable, @Param("estados") Set<EstadoOrdenCompra> estados);


    @Query(value = """
            select new com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO(
                o.id,
                o.codigoOrden,
                o.estado,
                p.nombre,
                o.fechaOrden,
                o.fechaCompromisoEntrega,
                o.descuento,
                coalesce(sum(d.cantidad), 0),
                coalesce(sum(d.cantidadRecibida), 0)
            )
            from OrdenCompra o
            join o.proveedor p
            left join o.detalles d
            where (:estado is null or o.estado = :estado)
              and (:proveedor is null or lower(p.nombre) like lower(concat('%', :proveedor, '%')))
              and (:atrasadas = false
                    or (
                        o.fechaCompromisoEntrega is not null
                        and o.fechaCompromisoEntrega < current_date
                        and o.estado in :estadosAtrasadas
                        and exists (
                            select 1 from OrdenCompraDetalle d1
                            where d1.ordenCompra = o and d1.cantidadRecibida < d1.cantidad
                        )
                    ))
            group by o.id, o.codigoOrden, o.estado, p.nombre, o.fechaOrden, o.fechaCompromisoEntrega, o.descuento
            """,
            countQuery = """
            select count(o) from OrdenCompra o
            join o.proveedor p
            where (:estado is null or o.estado = :estado)
              and (:proveedor is null or lower(p.nombre) like lower(concat('%', :proveedor, '%')))
              and (:atrasadas = false
                    or (
                        o.fechaCompromisoEntrega is not null
                        and o.fechaCompromisoEntrega < current_date
                        and o.estado in :estadosAtrasadas
                        and exists (
                            select 1 from OrdenCompraDetalle d1
                            where d1.ordenCompra = o and d1.cantidadRecibida < d1.cantidad
                        )
                    ))
            """)
    Page<OrdenCompraResponseDTO> findListadoFiltrado(Pageable pageable,
                                                     @Param("atrasadas") boolean atrasadas,
                                                     @Param("estadosAtrasadas") Set<EstadoOrdenCompra> estadosAtrasadas,
                                                     @Param("estado") EstadoOrdenCompra estado,
                                                     @Param("proveedor") String proveedor);
}
