package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.dto.InsumoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.willyes.clemenintegra.inventario.dto.StockDisponibleProjection;

import java.util.Collection;
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
    List<Producto> findByCategoriaProducto_TipoAndActivoTrueOrderByNombreAsc(TipoCategoria tipo);

    List<Producto> findByModoControlInventarioAndActivoTrue(ModoControlInventario modo);

    List<Producto> findByModoControlInventarioAndActivoTrueAndCategoriaProducto_TipoIn(
            ModoControlInventario modo,
            java.util.Collection<TipoCategoria> tipos
    );

    Optional<Producto> findByCodigoSku(String codigoSku);
    Optional<Producto> findByNombre(String nombre);

    boolean existsByCodigoSkuAndIdNot(String codigoSku, Long id);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
            "plantillaAnalisisMicrobiologico",
            "unidadMedida",
            "categoriaProducto"
    })
    Optional<Producto> findById(Long id);

    @Query("""
    select p
    from Producto p
    where (:activo is null or p.activo = :activo)
      and (
            :q is null or :q = ''
         or lower(p.nombre) like lower(concat('%', :q, '%'))
         or lower(p.codigoSku) like lower(concat('%', :q, '%'))
         or lower(concat(p.nombre, ' (', p.codigoSku, ')')) like lower(concat('%', :q, '%'))
      )
    """)
    Page<Producto> buscarPorTexto(@Param("q") String q, @Param("activo") Boolean activo, Pageable pageable);

    @Query("""
    select distinct p
    from Producto p
    join LoteProducto l on l.producto = p
    where l.almacen.id = :almacenId
      and (
            :q is null or :q = ''
         or lower(p.nombre) like lower(concat('%', :q, '%'))
         or lower(p.codigoSku) like lower(concat('%', :q, '%'))
         or lower(concat(p.nombre, ' (', p.codigoSku, ')')) like lower(concat('%', :q, '%'))
      )
    """)
    Page<Producto> buscarParaConteo(@Param("q") String q, @Param("almacenId") Long almacenId, Pageable pageable);

    /**
     * Busca insumos (MP, ME, suministros y semielaborados) por nombre.
     */
    @Query("""
            select p
            from Producto p
            where lower(p.nombre) like lower(concat('%', :term, '%'))
              and p.categoriaProducto.tipo in ('MATERIA_PRIMA','MATERIAL_EMPAQUE','SUMINISTROS','PRODUCTO_SEMI_ELABORADO')
            """)
    Page<Producto> searchInsumosByNombre(@Param("term") String term, Pageable pageable);

    /**
     * Busca productos terminados por nombre.
     */
    @Query("""
            select p
            from Producto p
            where lower(p.nombre) like lower(concat('%', :term, '%'))
              and p.categoriaProducto.tipo = 'PRODUCTO_TERMINADO'
            """)
    Page<Producto> searchProductosTerminadosByNombre(@Param("term") String term, Pageable pageable);

    @Query(value = """
            SELECT p.id AS productoId,
                   COALESCE(SUM(CASE WHEN lp.estado IN ('DISPONIBLE','LIBERADO')
                                     AND lp.agotado = false
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
                                     AND lp.agotado = false
                                     AND lp.almacenes_id IN (?2)
                                     AND (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
                                     THEN (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) ELSE 0 END), 0) AS stockDisponible
            FROM productos p
            LEFT JOIN lotes_productos lp ON lp.productos_id = p.id
            WHERE p.id IN (?1)
            GROUP BY p.id
            """, nativeQuery = true)
    List<StockDisponibleProjection> calcularStockDisponiblePorProductoEnAlmacenes(List<Long> ids, List<Long> almacenes);

    @Query(value = """
    SELECT new com.willyes.clemenintegra.inventario.dto.InsumoAutocompleteDTO(
        p.id,
        p.codigoSku,
        p.nombre,
        um.nombre,
        um.id,
        um.nombre
    )
    FROM Producto p
    JOIN p.unidadMedida um
    WHERE p.categoriaProducto.tipo IN :tipos
      AND p.activo = true
      AND (
           UPPER(p.nombre) LIKE CONCAT('%', UPPER(:term), '%')
        OR UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:term), '%')
      )
    """, countQuery = """
    SELECT COUNT(p)
    FROM Producto p
    WHERE p.categoriaProducto.tipo IN :tipos
      AND p.activo = true
      AND (
           UPPER(p.nombre) LIKE CONCAT('%', UPPER(:term), '%')
        OR UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:term), '%')
      )
    """)
    Page<InsumoAutocompleteDTO> buscarInsumosAutocomplete(
            @Param("tipos") Collection<TipoCategoria> tipos,
            @Param("term") String term,
            Pageable pageable
    );

    @Query(value = """
    SELECT new com.willyes.clemenintegra.inventario.dto.ProductoAutocompleteDTO(
        p.id,
        p.codigoSku,
        p.nombre
    )
    FROM Producto p
    WHERE p.categoriaProducto.tipo IN :tipos
      AND p.activo = true
      AND (
           UPPER(p.nombre) LIKE CONCAT('%', UPPER(:term), '%')
        OR UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:term), '%')
      )
    ORDER BY p.codigoSku ASC
    """, countQuery = """
    SELECT COUNT(p)
    FROM Producto p
    WHERE p.categoriaProducto.tipo IN :tipos
      AND p.activo = true
      AND (
           UPPER(p.nombre) LIKE CONCAT('%', UPPER(:term), '%')
        OR UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:term), '%')
      )
    """)
    Page<ProductoAutocompleteDTO> buscarFabricablesAutocomplete(
            @Param("tipos") Collection<TipoCategoria> tipos,
            @Param("term") String term,
            Pageable pageable
    );

    @Query(value = """
    SELECT new com.willyes.clemenintegra.inventario.dto.ProductoAutocompleteDTO(
        p.id,
        p.codigoSku,
        p.nombre,
        new com.willyes.clemenintegra.inventario.dto.UnidadMedidaAutocompleteDTO(
            um.id,
            um.nombre,
            um.simbolo,
            2
        )
    )
    FROM Producto p
    JOIN p.unidadMedida um
    WHERE p.activo = true
      AND (
           UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:query), '%')
        OR UPPER(p.nombre) LIKE CONCAT('%', UPPER(:query), '%')
      )
    """, countQuery = """
    SELECT COUNT(p)
    FROM Producto p
    WHERE p.activo = true
      AND (
           UPPER(p.codigoSku) LIKE CONCAT('%', UPPER(:query), '%')
        OR UPPER(p.nombre) LIKE CONCAT('%', UPPER(:query), '%')
      )
    """)
    Page<ProductoAutocompleteDTO> buscarAutocompleteInventarioAjustes(
            @Param("query") String query,
            Pageable pageable
    );

}
