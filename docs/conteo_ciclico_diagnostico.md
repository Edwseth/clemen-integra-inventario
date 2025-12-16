# Diagnóstico Conteo Cíclico – listado de lotes vs. stock sistema

## Reproducción mínima (IDs reales del caso)
Se ejecutaron las consultas solicitadas con `productoId=2` y `almacenId=3` (Obsoletos) sobre un dataset equivalente para el conteo `conteoId=2`. Los resultados confirman que el cálculo de stock suma 500 unidades y la consulta de lotes devuelve un registro con el mismo lote:

- Consulta A (stockSistema): `500`
- Consulta B (listado de lotes): un lote `L-OBSO-001` con `stock_lote=500` en `almacenes_id=3` y `productos_id=2`.

Salida directa de las consultas:
```
Query A: [(500.0,)]
Query B: [(1, 'L-OBSO-001', 'DISPONIBLE', 0, 500.0, 3, 2, None)]
```
【c565c2†L1-L33】

## Diferencia de filtros entre stockSistema y el endpoint `/lotes`
- **Stock (snapshot en conteo):** `sumarStockPorProductoYAlmacen` sólo filtra por `productoId`, `almacenId` y `ubicacionId` opcional; no impone estado, agotado ni texto.【F:src/main/java/com/willyes/clemenintegra/inventario/repository/LoteProductoRepository.java†L178-L187】【F:src/main/java/com/willyes/clemenintegra/inventario/service/ConteoCiclicoService.java†L349-L356】
- **Listado de lotes (endpoint `/api/inventario/conteos/{id}/lotes`):** la query añade `lp.estado in :estados` y un filtro opcional por texto (`upper(codigoLote) like ...`). El servicio siempre pasa `EnumSet.allOf(EstadoLote)` como estados visibles.【F:src/main/java/com/willyes/clemenintegra/inventario/repository/LoteProductoRepository.java†L189-L204】【F:src/main/java/com/willyes/clemenintegra/inventario/service/ConteoCiclicoService.java†L41-L113】

**Causa raíz:** el endpoint `/lotes` aplica filtros adicionales (estado permitido y búsqueda por texto) que no existen en el cálculo de `stockSistema`. Si los lotes en Obsoletos tienen un `estado` fuera del `EnumSet` (p. ej. valor legacy) o el front envía un `q` que no coincide con el `codigo_lote`, la lista puede quedar vacía aun cuando el stock se sume (>0).

## Punto de código y trazas
- Construcción del listado y logs DEBUG: `ConteoCiclicoService.listarLotesParaConteo` imprime `conteoId`, `almacenId`, `productoId`, `ubicacionFisicaId`, `search` y el tamaño final de la lista.【F:src/main/java/com/willyes/clemenintegra/inventario/service/ConteoCiclicoService.java†L103-L114】 
- Query con filtro de estado/texto: `LoteProductoRepository.buscarParaConteo` aplica `lp.estado in :estados` y `codigoLote` con `LIKE`.【F:src/main/java/com/willyes/clemenintegra/inventario/repository/LoteProductoRepository.java†L189-L204】
- Cálculo de stock del conteo: `sumarStockPorProductoYAlmacen` sin filtros adicionales.【F:src/main/java/com/willyes/clemenintegra/inventario/repository/LoteProductoRepository.java†L178-L187】

## Ajuste mínimo sugerido
Reutilizar exactamente los filtros base de stock (`productoId`, `almacenId`, `ubicacionId`) en `/lotes` para conteo. Opciones:
- Eliminar/relajar `lp.estado in :estados` o poblar `:estados` con los estados realmente presentes en la BD (incluyendo legacy) para Obsoletos.
- Evitar aplicar `q` cuando llegue vacío/`null` (ya se normaliza) y documentar que un `q` no coincidente vacía la lista.

Añadir un test donde `sumarStockPorProductoYAlmacen` devuelve >0 y `buscarParaConteo` retorne al menos un lote, garantizando la alineación de filtros.
