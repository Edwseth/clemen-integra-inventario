package com.willyes.clemenintegra.inventario.domain.repository;

import com.willyes.clemenintegra.inventario.domain.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}

