package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {
}

