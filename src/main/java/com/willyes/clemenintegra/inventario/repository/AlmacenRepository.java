package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    List<Almacen> findByTipoAndCategoria(TipoAlmacen tipo, TipoCategoria categoria);
    Page<Almacen> findByTipoAndCategoria(TipoAlmacen tipo, TipoCategoria categoria, Pageable pageable);
    boolean existsByNombre(String nombre);

    Optional<Almacen> findByNombre(String nombre);

}

