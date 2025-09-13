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
import com.willyes.clemenintegra.inventario.dto.StockDisponibleProjection;

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

    @Query(value = """
            SELECT p.id AS productoId,
                   COALESCE(SUM(CASE WHEN lp.estado IN ('DISPONIBLE','LIBERADO')
                                     AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
                                     THEN (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) ELSE 0 END), 0) AS stockDisponible
            FROM productos p
            LEFT JOIN lotes_productos lp ON lp.productos_id = p.id
            WHERE p.id IN (?1)
            GROUP BY p.id
            """, nativeQuery = true)
    List<StockDisponibleProjection> calcularStockDisponiblePorProducto(List<Long> ids);

    @Query(value = """
            SELECT p.id AS productoId,
                   COALESCE(SUM(CASE WHEN lp.estado IN ('DISPONIBLE','LIBERADO')
                                     AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
                                     THEN (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) ELSE 0 END), 0) AS stockDisponible
            FROM productos p
            LEFT JOIN lotes_productos lp ON lp.productos_id = p.id
            WHERE p.id IN (?1) AND lp.almacenes_id IN (?2)
            GROUP BY p.id
            """, nativeQuery = true)
    List<StockDisponibleProjection> calcularStockDisponiblePorProductoEnAlmacenes(List<Long> ids, List<Long> almacenes);
}

