package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long>, JpaSpecificationExecutor<OrdenProduccion> {

    @EntityGraph(attributePaths = {"producto", "producto.categoriaProducto", "unidadMedida", "responsable"})
    // Los filtros por relaciones (p. ej. nombre de producto) se resuelven desde Specification.
    Page<OrdenProduccion> findAll(Specification<OrdenProduccion> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"producto", "producto.categoriaProducto", "unidadMedida", "responsable"})
    List<OrdenProduccion> findAll(Specification<OrdenProduccion> spec, Sort sort);

    Optional<OrdenProduccion> findByLoteProduccion(String loteProduccion);

    @EntityGraph(attributePaths = {"producto", "producto.categoriaProducto", "unidadMedida", "responsable"})
    Optional<OrdenProduccion> findByCodigoOrdenIgnoreCase(String codigoOrden);

    Long countByCodigoOrdenStartingWith(String prefijo);

    @Query("select op.codigoOrden from OrdenProduccion op where op.codigoOrden like concat(:prefijo, '-%')")
    List<String> findCodigosByPrefijo(@Param("prefijo") String prefijo);

    Optional<OrdenProduccion> findTopByLoteProduccionStartingWithOrderByLoteProduccionDesc(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenProduccion o where o.id = :id")
    Optional<OrdenProduccion> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"producto", "producto.categoriaProducto", "unidadMedida", "responsable"})
    @Query("select op from OrdenProduccion op where op.id = :id")
    Optional<OrdenProduccion> findByIdWithProductoCategoria(@Param("id") Long id);

    @EntityGraph(attributePaths = {"producto", "producto.categoriaProducto", "producto.unidadMedida", "unidadMedida", "responsable"})
    @Query("select op from OrdenProduccion op where op.id = :id")
    Optional<OrdenProduccion> findByIdForCierreResponse(@Param("id") Long id);

    List<OrdenProduccion> findByFechaFinBetween(LocalDateTime inicio, LocalDateTime fin);

    List<OrdenProduccion> findByEstadoNotInAndFechaFinBetween(Collection<EstadoProduccion> estados, LocalDateTime inicio, LocalDateTime fin);

    List<OrdenProduccion> findByEstadoNotInAndFechaFinBefore(Collection<EstadoProduccion> estados, LocalDateTime limite);

    @EntityGraph(attributePaths = {"producto", "unidadMedida", "responsable"})
    List<OrdenProduccion> findByPlanProduccionDetalleIdIn(Set<Long> planDetalleIds);

}
