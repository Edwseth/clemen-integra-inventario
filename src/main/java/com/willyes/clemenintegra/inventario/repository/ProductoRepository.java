package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {
    boolean existsByCodigoSku(String codigoSku);
    boolean existsByNombre(String nombre);
    boolean existsByUnidadMedida(UnidadMedida unidadMedida);
    boolean existsByCategoriaProducto(CategoriaProducto categoriaProducto);

    List<Producto> findByCategoriaProducto_Tipo(TipoCategoria tipo);
    List<Producto> findByCategoriaProducto_Tipo(String tipo);
    List<Producto> findByCategoriaProducto_TipoIn(List<TipoCategoria> tipos);

    Optional<Producto> findByCodigoSku(String codigoSku);
    Optional<Producto> findByNombre(String nombre);

    boolean existsByCodigoSkuAndIdNot(String codigoSku, Long id);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    @Query("""
    select p
    from Producto p
    where (:q is null or :q = '' 
           or lower(p.nombre) like lower(concat('%', :q, '%'))
           or lower(p.codigoSku) like lower(concat('%', :q, '%')))
    """)
    Page<Producto> buscarPorTexto(@Param("q") String q, Pageable pageable);
}

