package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    List<Almacen> findByTipoAndCategoria(TipoAlmacen tipo, TipoCategoria categoria);
    boolean existsByNombre(String nombre);
}

