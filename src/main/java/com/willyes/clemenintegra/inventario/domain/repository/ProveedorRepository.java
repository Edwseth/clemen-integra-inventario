package com.willyes.clemenintegra.inventario.domain.repository;

import com.willyes.clemenintegra.inventario.domain.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
