package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long>, JpaSpecificationExecutor<AjusteInventario> {
}
