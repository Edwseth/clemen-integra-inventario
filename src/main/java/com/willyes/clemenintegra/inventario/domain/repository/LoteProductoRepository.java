package com.willyes.clemenintegra.inventario.domain.repository;

import com.willyes.clemenintegra.inventario.domain.model.LoteProducto;
import com.willyes.clemenintegra.inventario.domain.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {

    boolean existsByProducto(Producto producto);
}

