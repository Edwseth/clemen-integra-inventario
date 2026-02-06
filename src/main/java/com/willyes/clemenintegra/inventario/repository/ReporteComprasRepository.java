package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReporteComprasRepository extends Repository<OrdenCompraDetalle, Long> {

    @Query(value = """
        SELECT
            oc.codigo_orden                              AS ocCodigo,
            p.codigo_sku                                 AS productoCodigo,
            p.nombre                                     AS productoNombre,
            COALESCE(um.simbolo_impresion, um.simbolo)  AS udm,
            d.cantidad                                   AS cantidad,
            oc.fecha_orden                               AS fechaOc,
            oc.fecha_compromiso_entrega                  AS fechaPactada,
            COALESCE(
                MAX(ro.fecha_recepcion),
                MAX(ro2.fecha_recepcion)
            )                                            AS fechaRecepcion,
            pr.proveedor                                 AS proveedorNombre,
            oc.condiciones_pago                          AS condicionesPago,
            d.valor_unitario                             AS precioUnitario,
            d.iva                                        AS iva
        FROM orden_compra_detalle d
        JOIN ordenes_compra oc ON oc.id = d.ordenes_compra_id
        JOIN productos p ON p.id = d.productos_id
        JOIN unidades_medida um ON um.id = p.unidades_medida_id
        JOIN proveedores pr ON pr.id = oc.proveedor_id

        -- Fuente preferida: recepciones explícitas por ítem (si existen)
        LEFT JOIN recepciones_oc_detalles rod ON rod.orden_compra_detalle_id = d.id
        LEFT JOIN recepciones_oc ro ON ro.id = rod.recepcion_oc_id

        -- Fallback: movimientos que tienen recepcion_oc_id (tu data real sí lo tiene)
        LEFT JOIN movimientos_inventario m
               ON m.orden_compra_detalle_id = d.id
              AND m.recepcion_oc_id IS NOT NULL
        LEFT JOIN recepciones_oc ro2 ON ro2.id = m.recepcion_oc_id

        WHERE oc.fecha_orden BETWEEN :desde AND :hasta
        GROUP BY
            d.id,
            oc.codigo_orden,
            p.codigo_sku,
            p.nombre,
            COALESCE(um.simbolo_impresion, um.simbolo),
            d.cantidad,
            oc.fecha_orden,
            oc.fecha_compromiso_entrega,
            pr.proveedor,
            oc.condiciones_pago,
            d.valor_unitario,
            d.iva
        ORDER BY oc.fecha_orden DESC, oc.codigo_orden DESC
        """, nativeQuery = true)
    List<ReporteComprasRowProjection> obtenerReporte(@Param("desde") LocalDateTime desde,
                                                     @Param("hasta") LocalDateTime hasta);
}
