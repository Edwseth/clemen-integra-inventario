# Gestión de stock

El campo `stock_actual` de la tabla `productos` fue eliminado. El stock disponible se calcula dinámicamente sumando los lotes disponibles (`stock_lote - stock_reservado`).

## Consulta de stock disponible

```sql
SELECT p.id AS producto_id,
       COALESCE(SUM(CASE WHEN lp.estado='DISPONIBLE' AND lp.agotado=0
                         THEN (lp.stock_lote - lp.stock_reservado) ELSE 0 END), 0) AS stock_disponible
FROM productos p
LEFT JOIN lotes_productos lp ON lp.productos_id = p.id
WHERE p.id IN (:ids)
GROUP BY p.id;
```

## Índice FEFO y escala

Para optimizar la asignación multi-lote se recomienda mantener un índice compuesto que respete el orden FEFO:

```sql
CREATE INDEX IF NOT EXISTS idx_lotes_productos_fefo
ON lotes_productos (productos_id, estado, agotado, fecha_vencimiento, fecha_fabricacion, id);
```

Las reservas consumen cantidades con la misma escala definida en `stock_lote` y `stock_reservado` (DECIMAL), sin redondeos adicionales.
