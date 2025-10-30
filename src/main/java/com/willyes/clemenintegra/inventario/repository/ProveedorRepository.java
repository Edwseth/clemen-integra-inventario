package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByIdentificacion(String identificacion);
    boolean existsByIdentificacion(String identificacion);
    boolean existsByEmail(String email);

    @Query("SELECT p FROM Proveedor p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<Proveedor> findByProveedorContainingIgnoreCase(@Param("term") String term, Pageable pageable);

}
