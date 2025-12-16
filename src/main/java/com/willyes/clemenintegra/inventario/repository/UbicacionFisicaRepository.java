package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UbicacionFisicaRepository extends JpaRepository<UbicacionFisica, Long> {

    List<UbicacionFisica> findByAlmacenIdAndActivoTrueOrderByCodigoAsc(Integer almacenId);

    boolean existsByAlmacenIdAndCodigoIgnoreCase(Integer almacenId, String codigo);

    boolean existsByAlmacenIdAndCodigoIgnoreCaseAndIdNot(Integer almacenId, String codigo, Long id);

    @Query("""
            SELECT u
            FROM UbicacionFisica u
            WHERE u.activo = true
              AND (:almacenId IS NULL OR u.almacen.id = :almacenId)
              AND (
                :q IS NULL OR :q = '' OR
                LOWER(u.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(u.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY u.codigo ASC
            """)
    List<UbicacionFisica> searchActivas(
            @Param("almacenId") Integer almacenId,
            @Param("q") String q);

    Optional<UbicacionFisica> findByIdAndActivoTrue(Long id);
}
