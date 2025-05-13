package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}

