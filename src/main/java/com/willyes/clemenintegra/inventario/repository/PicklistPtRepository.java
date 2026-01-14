package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.PicklistPt;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PicklistPtRepository extends JpaRepository<PicklistPt, Long> {

    @EntityGraph(attributePaths = {"lineas", "lineas.producto", "lineas.loteProducto"})
    Optional<PicklistPt> findWithLineasById(Long id);

    @EntityGraph(attributePaths = {"asignaciones", "asignaciones.producto", "asignaciones.producto.unidadMedida",
            "asignaciones.loteProducto", "asignaciones.loteProducto.almacen", "asignaciones.loteProducto.ubicacionFisica"})
    Optional<PicklistPt> findWithAsignacionesById(Long id);

    Optional<PicklistPt> findTopByCodigoStartingWithOrderByCodigoDesc(String prefix);

    @Query("""
        select p
        from PicklistPt p
        where (:estado is null or p.estado = :estado)
          and (:cliente is null or lower(p.clienteNombre) like lower(concat('%', :cliente, '%')))
          and (:desde is null or p.fechaCreacion >= :desde)
          and (:hasta is null or p.fechaCreacion <= :hasta)
        order by p.fechaCreacion desc
    """)
    Page<PicklistPt> buscar(@Param("estado") PicklistPtEstado estado,
                            @Param("cliente") String cliente,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            Pageable pageable);
}
