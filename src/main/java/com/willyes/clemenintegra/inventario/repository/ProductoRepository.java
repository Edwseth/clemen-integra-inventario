package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCodigoSku(String codigoSku);
    boolean existsByNombre(String nombre);
    boolean existsByUnidadMedida(UnidadMedida unidadMedida);
    boolean existsByCategoriaProducto(CategoriaProducto categoriaProducto);

    List<Producto> findByCategoriaProducto_Tipo(TipoCategoria tipo);

    Optional<Producto> findByCodigoSku(String codigoSku);
}

