package com.willyes.clemenintegra.inventario.domain.repository;

import com.willyes.clemenintegra.inventario.domain.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.domain.model.Producto;
import com.willyes.clemenintegra.inventario.domain.model.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCodigoSku(String codigoSku);
    boolean existsByNombre(String nombre);
    boolean existsByUnidadMedida(UnidadMedida unidadMedida);
    boolean existsByCategoriaProducto(CategoriaProducto categoriaProducto);

}

