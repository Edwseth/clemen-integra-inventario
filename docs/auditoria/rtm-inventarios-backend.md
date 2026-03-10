# Auditoría técnica backend — Módulo Inventarios (Nivel B)

## Alcance y método

- **Repositorio analizado:** backend Spring Boot (`/workspace/permisos-back`).
- **Nivel de auditoría:** **Nivel B (Funcional + Técnica)**.
- **Cobertura de análisis:** Controllers, Services, Repositories, Entities/Model, DTOs y Mappers del dominio `inventario`.
- **Restricción encontrada:** en el repositorio no está disponible el archivo Excel URS (hoja `01_Inventarios`), por lo que los IDs se trazan como `INV-TR-XXX` para auditoría técnica y deben homologarse luego contra el URS oficial.

---

## 1) Escaneo backend de funcionalidades inventarios

### Paquetes y componentes clave identificados

- **Controllers** (ejemplos):
  - `MovimientoInventarioController`
  - `ProductoController`
  - `LoteProductoController`
  - `AjusteInventarioController`
  - `KardexController`
  - `ConteoCiclicoController`
  - `InventarioFefoController`
  - `InventarioConsultaController`
  - `AlertaInventarioController`
  - `ReporteInventarioController`

- **Services** (ejemplos):
  - `MovimientoInventarioServiceImpl`
  - `ProductoServiceImpl`
  - `LoteProductoServiceImpl`
  - `AjusteInventarioServiceImpl`
  - `KardexServiceImpl`
  - `ConteoCiclicoService`
  - `InventarioConsultaServiceImpl`

- **Repositories** (ejemplos):
  - `MovimientoInventarioRepository`
  - `ProductoRepository`
  - `LoteProductoRepository`
  - `AjusteInventarioRepository`
  - `ConteoCiclicoRepository`

- **Model/Entities** (ejemplos):
  - `MovimientoInventario`
  - `Producto`
  - `LoteProducto`
  - `AjusteInventario`
  - `ConteoCiclico` / `ConteoCiclicoDetalle`

- **DTOs/Mappers** (ejemplos):
  - `MovimientoInventarioDTO`, `MovimientoInventarioResponseDTO`
  - `ProductoRequestDTO`, `ProductoResponseDTO`
  - `LoteProductoRequestDTO`, `LoteProductoResponseDTO`
  - `AjusteInventarioRequestDTO`, `AjusteInventarioResponseDTO`
  - mappers: `MovimientoInventarioMapper`, `ProductoMapper`, `LoteProductoMapper`, `AjusteInventarioMapper`, `ConteoCiclicoMapper`

---

## 2) Endpoints backend relacionados con inventarios

### Núcleo inventarios

- `POST /api/movimientos`
- `GET /api/movimientos`
- `GET /api/movimientos/filtrar`
- `GET /api/movimientos/buscar`

- `GET /api/productos` + CRUD y búsquedas (`/buscar`, `/buscar-insumos`, `/buscar-pt`, etc.)
- `GET/POST /api/lotes` + operaciones de estado (`/liberar`, `/rechazar`, `/liberar-retenido`)
- `GET/POST/DELETE /api/inventario/ajustes`
- `GET /api/inventario/kardex`
- `GET /api/inventario/fefo/preview`
- `GET /api/inventario/disponibilidad/producto/{productoId}`
- `GET /api/inventario/alertas` + subrutas de alertas

### Flujo operativo de conteos

- `GET/POST /api/inventario/conteos`
- `POST /api/inventario/conteos/{id}/detalles`
- `POST /api/inventario/conteos/{id}/iniciar`
- `POST /api/inventario/conteos/{id}/cerrar`
- `POST /api/inventario/conteos/{id}/aplicar`

### Reportería

- `GET /api/reportes/*` (stock, inventario general, movimientos, vencimientos, trazabilidad, etc.)
- `GET /api/inventario/reportes/conteos-ajuste`
- `GET /api/inventario/reportes/conteos-ajuste/excel`

---

## 3) Matriz de auditoría (Nivel B)

| Req ID | Descripción | Evidencia Backend | Estado | Observaciones |
|---|---|---|---|---|
| INV-TR-001 | Registrar movimientos de inventario (entrada/salida/transferencia) | `MovimientoInventarioController`, `MovimientoInventarioServiceImpl`, endpoint `/api/movimientos` | IMPLEMENTADO | Existe endpoint + lógica de negocio de registro transaccional. |
| INV-TR-002 | Validar stock antes de salidas/ajustes | `MovimientoInventarioController` (pre-check stock), `StockQueryService`, `MovimientoInventarioServiceImpl` | IMPLEMENTADO | Se valida disponibilidad por producto/lote; retorna conflicto funcional cuando no alcanza. |
| INV-TR-003 | Control de idempotencia en registro de movimientos | `MovimientoInventarioController` (`Idempotency-Key`), `MovimientoInventarioServiceImpl` (`findByIdempotencyKey`) | IMPLEMENTADO | Previene duplicados operativos por reintentos. |
| INV-TR-004 | Gestión de productos de inventario (CRUD + búsquedas) | `ProductoController`, `ProductoServiceImpl`, `ProductoRepository` | IMPLEMENTADO | CRUD disponible con validaciones funcionales (duplicados, categoría/unidad, reglas de rendimiento). |
| INV-TR-005 | Gestión de lotes (alta, consulta, filtros por estado/fechas) | `LoteProductoController`, `LoteProductoServiceImpl`, `LoteProductoRepository` | IMPLEMENTADO | Filtros y paginación sanitizada; validaciones de rango de fechas y estado. |
| INV-TR-006 | Flujo de decisión de calidad sobre lote (liberar/rechazar/liberar retenido) | `LoteProductoController` (`PUT /{id}/liberar`, `/rechazar`, `/liberar-retenido`), `LoteProductoServiceImpl` | IMPLEMENTADO | Se implementa operación de decisión con observación opcional y reglas de servicio. |
| INV-TR-007 | Aplicar FEFO/FIFO para consumo sugerido de lotes | `InventarioFefoController`, `MovimientoInventarioService.simulateFefo` | IMPLEMENTADO | Endpoint de simulación con validación de parámetros y cálculo de consumo por lotes. |
| INV-TR-008 | Kardex de movimientos con saldo acumulado y filtros | `KardexController`, `KardexServiceImpl`, endpoint `/api/inventario/kardex` | IMPLEMENTADO | Incluye validaciones de parámetros y cálculo secuencial de saldo. |
| INV-TR-009 | Conteo cíclico con workflow (crear, detallar, cerrar, aplicar) | `ConteoCiclicoController`, `ConteoCiclicoService`, repositorios de conteo/detalle | IMPLEMENTADO | Flujo completo con estados, validaciones, idempotencia en aplicar y generación de ajustes por diferencia. |
| INV-TR-010 | Ajustes de inventario vinculados a movimiento real | `AjusteInventarioController`, `AjusteInventarioServiceImpl`, `MovimientoInventarioService` | IMPLEMENTADO | Al crear ajuste se registra movimiento de inventario asociado y se validan lote/almacén/producto. |
| INV-TR-011 | Alertas de inventario (stock bajo, vencidos, retenidos prolongados) | `AlertaInventarioController`, `AlertaInventarioService` | IMPLEMENTADO | Endpoints específicos por tipo de alerta. |
| INV-TR-012 | Reportes de inventario exportables (Excel) | `ReporteInventarioController`, `ReporteInventarioService`, `InventarioReportesController` | IMPLEMENTADO | Múltiples reportes Excel implementados (inventario general, stock, vencimientos, movimientos, etc.). |
| INV-TR-013 | Trazabilidad de inventario/lotes con consulta operacional | `ReporteInventarioController` (`/trazabilidad-lote`) y `TrazabilidadLoteController` | PARCIAL | Hay export y controller dedicado; pendiente confirmar cobertura exacta de campos exigidos por URS oficial. |
| INV-TR-014 | Cierre de período de inventario / bloqueo contable de movimientos | No se identifica controller/service explícito de cierre de periodo en `inventario` | NO_IMPLEMENTADO | No se observa funcionalidad explícita en módulo inventarios para cierre contable de periodo (requiere validación con URS). |

---

## 4) Evidencia técnica (muestra representativa)

- **Movimientos**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/MovimientoInventarioController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/MovimientoInventarioServiceImpl.java`
  - Endpoint: `/api/movimientos`

- **Productos**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/ProductoController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/ProductoServiceImpl.java`
  - Endpoint base: `/api/productos`

- **Lotes**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/LoteProductoController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/LoteProductoServiceImpl.java`
  - Endpoint base: `/api/lotes`

- **Ajustes**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/AjusteInventarioController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/AjusteInventarioServiceImpl.java`
  - Endpoint base: `/api/inventario/ajustes`

- **Conteos cíclicos**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/ConteoCiclicoController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/ConteoCiclicoService.java`
  - Endpoint base: `/api/inventario/conteos`

- **Kardex**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/KardexController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/service/KardexServiceImpl.java`
  - Endpoint: `/api/inventario/kardex`

- **Alertas y reportes**
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/AlertaInventarioController.java`
  - `src/main/java/com/willyes/clemenintegra/inventario/controller/ReporteInventarioController.java`
  - Endpoints: `/api/inventario/alertas`, `/api/reportes/*`

---

## 5) Resumen de cumplimiento

### Requisitos implementados

- INV-TR-001, 002, 003, 004, 005, 006, 007, 008, 009, 010, 011, 012.

### Requisitos parciales

- INV-TR-013.

### Requisitos faltantes

- INV-TR-014.

---

## 6) Observaciones finales de auditoría

1. El backend de inventarios muestra **alta cobertura funcional** en operaciones críticas (movimientos, lotes, conteos, kardex, reportes).
2. Existe una base de validaciones técnica/funcional robusta (idempotencia, validación de stock, estados de workflow, filtros y validación de parámetros).
3. Para cerrar la RTM formal de URS, se requiere el archivo de requisitos oficial (`01_Inventarios`) para reemplazar esta codificación técnica (`INV-TR-XXX`) por los IDs definitivos del documento URS.
