package com.willyes.clemenintegra.inventario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface InventarioValorizadoRepository extends Repository<com.willyes.clemenintegra.inventario.model.LoteProducto, Long> {

    @Query(value = """
            SELECT p.codigo_sku AS sku,
                   p.nombre AS nombre,
                   COALESCE(um.simbolo_impresion, um.simbolo, um.nombre) AS udm,
                   lp.codigo_lote AS lote,
                   DATE(lp.fecha_vencimiento) AS vence,
                   TRIM(BOTH ' /' FROM CONCAT(COALESCE(uf.codigo, ''), ' / ', COALESCE(a.nombre, ''))) AS ubicacionAlmacen,
                   ROUND((lp.stock_lote - COALESCE(lp.stock_reservado, 0)), 6) AS stock,
                   COALESCE(lp.costo_unitario_material, 0) AS costoUnitarioMaterial,
                   ROUND((lp.stock_lote - COALESCE(lp.stock_reservado, 0)) * COALESCE(lp.costo_unitario_material, 0), 6) AS valorTotal
            FROM lotes_productos lp
            JOIN productos p ON p.id = lp.productos_id
            LEFT JOIN unidades_medida um ON um.id = p.unidades_medida_id
            LEFT JOIN almacenes a ON a.id = lp.almacenes_id
            LEFT JOIN ubicaciones_fisicas uf ON uf.id = lp.ubicaciones_fisicas_id
            WHERE (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
            ORDER BY p.codigo_sku ASC, lp.codigo_lote ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM lotes_productos lp
            WHERE (lp.stock_lote - COALESCE(lp.stock_reservado, 0)) > 0
            """,
            nativeQuery = true)
    Page<InventarioValorizadoRowProjection> findInventarioValorizado(Pageable pageable);
}
