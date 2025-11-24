# Fase 1 – Modelo de datos y contratos API para Planeación/MRP

## 1. Resumen ejecutivo
- Objetivo de la fase: definir los parámetros de planeación que se incorporarán en Producto, modelar las nuevas entidades del módulo Planeación/MRP (CorridaMrp y SugerenciaAbastecimiento) y fijar los contratos API iniciales para un MRP simple.
- No se modifica la lógica existente de Inventarios, Producción, Compras ni BOM; solo se diseñan los insumos para la próxima iteración.
- Los parámetros definidos habilitarán el cálculo de puntos de acción y cantidades sugeridas sin alterar aún los flujos de OC/OP.
- Se deja explícito el alcance: recomendaciones generadas por MRP que luego serán convertidas a órdenes reales en fases posteriores.

## 2. Nuevos parámetros en Producto
- Campos a agregar a la entidad `Producto`:
  - `Integer leadTimeCompraDias` → columna `lead_time_compra_dias` (INT)
  - `Integer leadTimeProduccionDias` → columna `lead_time_produccion_dias` (INT)
  - `BigDecimal stockSeguridad` → columna `stock_seguridad` (DECIMAL(19,6))
  - `BigDecimal stockMaximoPlaneacion` → columna `stock_maximo_planeacion` (DECIMAL(19,6))
- El campo existente `stockMinimo` se reutiliza como punto de reorden base.
- Solo se incluirán en el MRP los productos con `modoControlInventario = CONTROL_STOCK`.

| Campo Java | Tipo | Columna SQL | Propósito | Notas |
|------------|------|-------------|-----------|-------|
| `leadTimeCompraDias` | `Integer` | `lead_time_compra_dias` | Días estimados desde la orden hasta la recepción para insumos comprados. | Opcional; usado solo por MRP para sugerir fechas de pedido. |
| `leadTimeProduccionDias` | `Integer` | `lead_time_produccion_dias` | Días estimados desde la liberación de la OP hasta la disponibilidad del producto fabricado. | Opcional; aplica a productos fabricados. |
| `stockSeguridad` | `BigDecimal` | `stock_seguridad` | Colchón adicional para calcular el punto de acción (stockMínimo + stockSeguridad). | Opcional; si es nulo se asume 0. |
| `stockMaximoPlaneacion` | `BigDecimal` | `stock_maximo_planeacion` | Nivel objetivo superior para calcular la cantidad sugerida de reabastecimiento. | Si es nulo se aplica lógica L4L (igualar faltante). |

## 3. Nuevas entidades del módulo Planeación/MRP
- Paquete lógico: `com.willyes.clemenintegra.planeacion`.

### 3.1. CorridaMrp
- Campos: `id`, `fechaEjecucion`, `horizonteDesde`, `horizonteHasta`, `usuarioEjecutoId`, `resumen`.
- Relación `OneToMany` con `SugerenciaAbastecimiento`.
- Tabla sugerida: `corridas_mrp`.

### 3.2. SugerenciaAbastecimiento
- Campos: `id`, `corrida` (`ManyToOne`), `producto` (`ManyToOne`), `tipo`, `cantidadSugerida`, `fechaRequerida`, `fechaSugeridaPedido`, `estado`, `origen`, `observaciones`, `ordenCompraId`, `ordenProduccionId`.
- Tabla sugerida: `sugerencias_abastecimiento`.

### 3.3. Enums en `com.willyes.clemenintegra.planeacion.model.enums`
- `TipoSugerenciaAbastecimiento`: `COMPRA`, `PRODUCCION`.
- `EstadoSugerenciaAbastecimiento`: `PENDIENTE`, `APROBADA`, `DESCARTADA`, `CONVERTIDA`.
- `OrigenSugerenciaAbastecimiento`: `MRP_SIMPLE`, `MRP_OP`.

> Estas entidades no reemplazan a las órdenes de compra (OC) ni a las órdenes de producción (OP). Generan recomendaciones que deberán convertirse posteriormente en órdenes reales mediante acciones explícitas.

## 4. DTOs de respuesta
- `SugerenciaAbastecimientoResponseDTO` (futuro):
  - `id`, `corridaId`, `productoId`, `productoSku`, `productoNombre`, `categoriaProducto`, `tipoSugerencia`, `cantidadSugerida`, `fechaRequerida`, `fechaSugeridaPedido`, `estado`, `origen`, `observaciones`.
- `CorridaMrpResponseDTO` (futuro):
  - `id`, `fechaEjecucion`, `horizonteDesde`, `horizonteHasta`, `modo`, `usuarioEjecutoId`, `resumen`, `totalSugerencias`.

Estos DTOs se implementarán en fases posteriores, pero se documentan ahora para fijar los contratos de salida del módulo.

## 5. Contratos API del módulo Planeación/MRP
- Controlador previsto: `com.willyes.clemenintegra.planeacion.controller.PlaneacionMrpController` con prefijo `/api/planeacion/mrp`.

### 5.1. `POST /api/planeacion/mrp/simple`
- **Request sugerido (JSON)**:
```json
{
  "horizonteDesde": "2024-09-01",
  "horizonteHasta": "2024-10-15",
  "categoriasProducto": ["MATERIA_PRIMA", "MATERIAL_EMPAQUE"],
  "soloControlStock": true,
  "incluirProductosSinParametros": false
}
```
- **Response sugerida**:
  - Objeto `corrida` (`CorridaMrpResponseDTO`) y lista `sugerencias` (`SugerenciaAbastecimientoResponseDTO`).
- **Reglas de negocio (diseño; implementación en Fase 2)**:
  - Calcular `stockActual` y `stockProyectado` usando lotes DISPONIBLE/LIBERADO no agotados y órdenes de compra abiertas.
  - Definir `puntoAccion = stockMinimo + stockSeguridad`.
  - Generar sugerencias cuando `stockProyectado < puntoAccion`.
  - Calcular `cantidadSugerida` usando `stockMaximoPlaneacion` si existe; de lo contrario, usar lógica L4L (cubrir faltante hasta el punto de acción).

### 5.2. `GET /api/planeacion/mrp/corridas`
- Paginación estándar (`page`, `size`, `sort`).
- Filtros: rango de fecha de ejecución, `modo` (`MRP_SIMPLE`, `MRP_OP`).
- Respuesta: lista paginada de `CorridaMrpResponseDTO`.

### 5.3. `GET /api/planeacion/mrp/corridas/{corridaId}/sugerencias`
- Filtros: `tipo` (`COMPRA`/`PRODUCCION`), `estado` (`PENDIENTE`, etc.), categoría de producto.
- Respuesta: lista paginada de `SugerenciaAbastecimientoResponseDTO`.

### 5.4. `GET /api/planeacion/mrp/sugerencias`
- Vista global de sugerencias con filtros por `estado`, `tipo`, `productoId`/`sku`, `corridaId`.
- Respuesta: lista paginada de `SugerenciaAbastecimientoResponseDTO`.

### Endpoints futuros (solo diseño)
- `POST /api/planeacion/mrp/sugerencias/{id}/aprobar`
- `POST /api/planeacion/mrp/sugerencias/{id}/descartar`
- `POST /api/planeacion/mrp/sugerencias/{id}/convertir-oc`
- `POST /api/planeacion/mrp/sugerencias/{id}/convertir-op`

## 6. Relación con módulos existentes y riesgos
- Reutilización prevista:
  - Lógica de disponibilidad de insumos de BOM/Producción (`FormulaProductoServiceImpl`, `DisponibilidadInsumoService`).
  - Cálculo de stock FEFO de Inventarios (`LoteProductoRepository`, `InventarioConsultaServiceImpl`).
  - Órdenes de compra como objetos de ejecución (`OrdenCompra`, `OrdenCompraController`).
- Riesgos y decisiones pendientes:
  - Unificar explícitamente los estados de lote "consumibles" para MRP.
  - Definir qué estados de OC cuentan como "por recibir" (`CREADA`, `ENVIADA`, `PARCIALMENTE_RECIBIDA`).
  - Revisar qué categorías de producto participan en MRP (`MATERIA_PRIMA`, `MATERIAL_EMPAQUE`, etc.).
