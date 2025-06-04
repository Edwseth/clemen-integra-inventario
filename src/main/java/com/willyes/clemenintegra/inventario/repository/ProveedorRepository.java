package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByIdentificacion(String identificacion);
    boolean existsByIdentificacion(String identificacion);
    boolean existsByEmail(String email);

}
