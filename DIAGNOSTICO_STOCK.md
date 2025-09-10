# Diagnóstico de gestión de stock

Este documento resume los hallazgos sobre el uso actual de `productos.stock_actual` en el backend y propone una proyección de `stockDisponible` basada en lotes.

## Endpoints afectados
- `GET /api/productos` y variantes (`/buscar`, `/categoria/{nombre}`, `/terminados`, etc.) desde `ProductoController`.
- `GET /api/productos/{id}` en `ProductoController`.
- `POST /api/movimientos` en `MovimientoInventarioController` (valida stock del producto y del lote).
- `GET /api/reportes/stock-actual` en `ReporteInventarioController`.
- Servicios de alertas y reportes que dependen de `ProductoServiceImpl` y `LoteProductoServiceImpl`.

## Clases involucradas
- **Entity/Repository**: `Producto`, `ProductoRepository`, `LoteProductoRepository`.
- **Service**: `ProductoServiceImpl`, `MovimientoInventarioServiceImpl`, `LoteProductoServiceImpl`, `AlertaInventarioServiceImpl`, `OrdenProduccionServiceImpl`.
- **Controller**: `ProductoController`, `MovimientoInventarioController`, `ReporteInventarioController`.
- **DTO/Mapper**: `ProductoResponseDTO`, `AlertaInventarioResponseDTO`, `ProductoAlertaResponseDTO`, `ProductoMapper`.

## Uso actual de `stock_actual`
- Se expone directamente en `ProductoResponseDTO` y se consulta para validar salidas en `MovimientoInventarioController`.
- Servicios como `AlertaInventarioServiceImpl` y `LoteProductoServiceImpl` comparan `stock_actual` contra `stock_minimo`.
- Reportes generan columnas basadas en `productos.stock_actual`.
- No se encontraron triggers ni jobs que sincronicen el campo con los lotes.

## Riesgos de cambio
- Frontend depende de `stockActual` en listados y detalle de productos; deberá migrar a `stockDisponible`.
- Cualquier lógica externa que actualice `productos.stock_actual` podría quedar desfasada.

## Consideraciones de rendimiento
- Se recomienda calcular el stock disponible vía query agregada sobre `lotes_productos`:

```sql
SELECT p.id AS producto_id,
       COALESCE(SUM(CASE WHEN lp.estado='DISPONIBLE' AND lp.agotado=0
                         THEN (lp.stock_lote - lp.stock_reservado) ELSE 0 END), 0) AS stock_disponible
FROM productos p
LEFT JOIN lotes_productos lp ON lp.productos_id = p.id
WHERE p.id IN (:ids)
GROUP BY p.id;
```

- Índices sugeridos:
  - `lotes_productos(productos_id, estado, agotado)`
  - `lotes_productos(productos_id, estado, agotado, fecha_vencimiento)` para consultas por vencimiento
  - `lotes_productos(productos_id, estado, agotado, fecha_vencimiento, fecha_fabricacion, id)` para selección FEFO
  - `lotes_productos(productos_id, almacenes_id)` para cortes por almacén

## Índice FEFO y escala

Para optimizar la asignación multi-lote se recomienda mantener un índice compuesto que respete el orden FEFO:

```sql
CREATE INDEX IF NOT EXISTS idx_lotes_productos_fefo
ON lotes_productos (productos_id, estado, agotado, fecha_vencimiento, fecha_fabricacion, id);
```

Las reservas consumen cantidades con la misma escala definida en `stock_lote` y `stock_reservado` (DECIMAL), sin redondeos adicionales.

